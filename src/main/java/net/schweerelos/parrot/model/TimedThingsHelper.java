/*
 * Copyright (C) 2012 Knut Müller,
 * Copyright (C) 2011 Andrea Schweer
 *
 * This file is part of the Digital Parrot with History. 
 *
 * The Digital Parrot is free software; you can redistribute it and/or modify
 * it under the terms of the Eclipse Public License as published by the Eclipse
 * Foundation or its Agreement Steward, either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * The Digital Parrot with history is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the Eclipse Public
 * License for more details.
 *
 * You should have received a copy of the Eclipse Public License along with the
 * Digital Parrot. If not, see http://www.eclipse.org/legal/epl-v10.html. 
 *
 */


package net.schweerelos.parrot.model;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import net.schweerelos.timeline.model.PayloadInterval;
import net.schweerelos.timeline.model.IntervalChain;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

import de.kmamut.parrot.history.changes.Version;

public class TimedThingsHelper {
	private static final String TIMED_THING = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#TimedThing";
	static private final String ABSOLUTELY_TIMED_THING = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#AbsolutelyTimedThing";
	static private final String ENDS_AT = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#endsAt";
	static private final String STARTS_AT = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#startsAt";
	private static final String DURING = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#during";

	public static IntervalChain<NodeWrapper> extractTimedThings(
			ParrotModel pModel) {
		OntModel model = pModel.getOntModel();
		Resource timedThingClass = model.createClass(TIMED_THING);

		IntervalChain<NodeWrapper> timelineModel = new IntervalChain<NodeWrapper>();
		
		ExtendedIterator<Individual> instances = NiceIterator.emptyIterator();
		if (pModel instanceof ParrotModelWithHistory)
			for (Entry<Version, OntModel> mapEntry : ((ParrotModelWithHistory) pModel).getVersionedOntModels().entrySet()) {
				model.createClass(TIMED_THING);
				instances = instances.andThen(mapEntry.getValue().listIndividuals(timedThingClass));
			}
		else
			instances = model.listIndividuals(timedThingClass);
		
		Set<NodeWrapper> processedNodes = new HashSet<NodeWrapper>();
		
		while (instances.hasNext()) {
			Individual instance = instances.next();
			final NodeWrapper node = pModel.getNodeWrapper(instance);

			if (processedNodes.contains(node))
				continue;
			processedNodes.add(node);
			
			DateTime startsAt;
			DateTime endsAt;
			try {
				startsAt = extractStartDate(instance);
			} catch (NotTimedThingException e) {
				// ignore this individual
				continue;
			}
			try {
				endsAt = extractEndDate(instance);
			} catch (NotTimedThingException e) {
				// ignore this individual
				continue;
			}

			if (startsAt == null || endsAt == null) {
				// only proceed if we have a start time *and* an end time
				continue;
			}
			
			if (endsAt.isBefore(startsAt)) {
				System.out.printf("end timestamp %s is before start timestamp %s, ignoring instance %s as TimedThing\n",
						endsAt.toString(), startsAt.toString(), instance.getURI());
				continue;
			}
			final Interval base = new Interval(startsAt, endsAt);

			PayloadInterval<NodeWrapper> interval = new PayloadInterval<NodeWrapper>() {
				Interval baseInterval = base;

				@Override
				public DateTime getEnd() {
					return baseInterval.getEnd();
				}

				@Override
				public DateTime getStart() {
					return baseInterval.getStart();
				}

				@Override
				public NodeWrapper getPayload() {
					return node;
				}

				@Override
				public boolean contains(Interval interval) {
					return baseInterval.contains(interval);
				}

				@Override
				public Interval toInterval() {
					return baseInterval;
				}

			};
			timelineModel.add(interval);
		}
		
		processedNodes.clear();
		
		return timelineModel;
	}

	/**
	 * Determines whether the node is a timedthing with reasonably well-known
	 * temporal boundaries. This is the case if the node is an instance of
	 * AbsolutelyTimedThing or if it occurred during an AbsolutelyTimedThing.
	 * 
	 * @param node
	 *            the node to check
	 * @param model
	 *            the ont model from which the node is taken
	 * @return true if the node is a timedthing with reasonably well-known
	 *         boundaries.
	 */
	public static boolean isTimedThing(OntResource node) {
		if (isAbsolutelyTimedThing(node)) {
			return true;
		}
		return isIndirectlyTimedThing(node);
	}

	public static DateTime extractStartDate(OntResource subject)
			throws NotTimedThingException {
		if (isAbsolutelyTimedThing(subject)) {
			return extractAbsoluteDate(subject, STARTS_AT);
		} else if (isIndirectlyTimedThing(subject)) {
			return extractNearestDate(subject, STARTS_AT, true);
		}
		throw new NotTimedThingException(subject + " is not a timed thing");
	}

	/**
	 * Extracts a {@code DateTime} representation of a date associated with the
	 * subject, by iterating through all spanning {@code AbsolutelyTimedThing}s
	 * and taking the nearest date. The type of date is specified via its
	 * property name.
	 * 
	 * @param subject
	 *            the subject for which the date should be extracted. The
	 *            assumption is that the subject is in indirectly timed thing
	 *            (ie {@code TimedThingsHelper#isIndirectlyTimedThing(subject,
	 *            model)} is true).
	 * @param propertyName
	 *            name of the property to use (would normally be {@code
	 *            TimedThingsHelper#STARTS_AT} or {@code
	 *            TimedThingsHelper#ENDS_AT})
	 * @param before
	 *            whether the nearest date should be before or after the thing's
	 *            time.
	 * @return a {@code DateTime} representation of a date associated with the
	 *         subject
	 * @throws NotTimedThingException
	 *             if the subject isn't timed
	 */
	private static DateTime extractNearestDate(OntResource subject,
			String propertyName, boolean before) throws NotTimedThingException {
		DateTime currentBestCandidate = null;

		Property duringProperty = subject.getOntModel().createProperty(DURING);
		// TODO #42 this should go through before and after as well
		if (!subject.hasProperty(duringProperty)) {
			throw new NotTimedThingException(subject
					+ " doesn't have property " + DURING);
		}
		NodeIterator values = subject.listPropertyValues(duringProperty);
		while (values.hasNext()) {
			RDFNode value = values.next();
			if (isAbsolutelyTimedThing(value)) {
				DateTime valueDate = extractAbsoluteDate(value, propertyName);
				boolean betterThanCurrentBest = false;
				if (currentBestCandidate == null) {
					betterThanCurrentBest = true;
				} else {
					betterThanCurrentBest = (valueDate
							.isBefore(currentBestCandidate) == before);
				}
				if (betterThanCurrentBest) {
					currentBestCandidate = valueDate;
				}
			}
		}
		return currentBestCandidate;
	}

	private static DateTime extractAbsoluteDate(RDFNode subject,
			String propertyName) throws NotTimedThingException {
		if (subject.canAs(OntResource.class)) {
			return extractAbsoluteDate(subject.as(OntResource.class), propertyName);
		}
		throw new NotTimedThingException(
				"can't extract date for nodes that aren't OntResources");
	}

	/**
	 * Extracts a {@code DateTime} representation of the subject's date. The
	 * type of date is specified via its property name.
	 * 
	 * @param subject
	 *            the subject for which a date should be extracted. The
	 *            assumption is that this is an absolutely timed thing, ie
	 *            {@code #isAbsolutelyTimedThing(subject, model)} is true.
	 * @param propertyName
	 *            name of the property to use (would normally be {@code
	 *            TimedThingsHelper#STARTS_AT} or {@code
	 *            TimedThingsHelper#ENDS_AT})
	 * @return a date representation of the subject's value of the specified
	 *         property
	 * @throws NotTimedThingException
	 *             if the subject isn't timed
	 */
	private static DateTime extractAbsoluteDate(OntResource subject,
			String propertyName) throws NotTimedThingException {
		Property prop = subject.getOntModel().createProperty(propertyName);
		RDFNode propValue = subject.getPropertyValue(prop);
		return extractDate(propValue);
	}

	public static DateTime extractEndDate(OntResource subject)
			throws NotTimedThingException {
		if (isAbsolutelyTimedThing(subject)) {
			return extractAbsoluteDate(subject, ENDS_AT);
		} else if (isIndirectlyTimedThing(subject)) {
			return extractNearestDate(subject, ENDS_AT, false);
		}
		throw new NotTimedThingException(subject + " is not a timed thing");
	}

	private static boolean isAbsolutelyTimedThing(RDFNode node) {
		if (!node.canAs(OntResource.class)) {
			return false;
		}
		OntResource res = node.as(OntResource.class);
		return isAbsolutelyTimedThing(res);
	}

	private static boolean isAbsolutelyTimedThing(OntResource node) {
		if (!node.isIndividual()) {
			return false;
		}
		Individual individual = node.asIndividual();
		return individual.hasOntClass(ABSOLUTELY_TIMED_THING);
	}

	/**
	 * check whether it happened during an AbsolutelyTimedThing
	 * 
	 * @param node
	 * @return
	 */
	private static boolean isIndirectlyTimedThing(OntResource node) {
		Property duringProperty = node.getOntModel().createProperty(DURING);
		if (!node.hasProperty(duringProperty)) {
			return false;
		}
		NodeIterator values = node.listPropertyValues(duringProperty);
		while (values.hasNext()) {
			RDFNode value = values.next();
			if (isAbsolutelyTimedThing(value)) {
				return true;
			}
		}
		return false;
	}

	private static DateTime extractDate(RDFNode node)
			throws NotTimedThingException {
		if (!node.isLiteral()) {
			throw new NotTimedThingException("Node is not a literal");
		}
		RDFDatatype type = ((Literal) node).getDatatype();
		Object value = ((Literal) node).getValue();
		if (type != null && value != null && value instanceof XSDDateTime) {
			return new DateTime(((XSDDateTime) value).asCalendar());
		} else {
			throw new NotTimedThingException("Node does not represent a date");
		}
	}

}
