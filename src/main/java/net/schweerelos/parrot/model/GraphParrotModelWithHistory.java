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

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import net.schweerelos.parrot.model.filters.ChainLink;
import net.schweerelos.parrot.model.filters.VersionFilter;
import net.schweerelos.parrot.util.QuadTree;
import net.schweerelos.timeline.model.IntervalChain;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mindswap.pellet.jena.ModelExtractor;
import org.mindswap.pellet.jena.PelletInfGraph;
import org.mindswap.pellet.jena.ModelExtractor.StatementType;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import de.kmamut.parrot.history.HistoryModel;
import de.kmamut.parrot.history.changes.ChangeType;
import de.kmamut.parrot.history.changes.Version;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;

public class GraphParrotModelWithHistory extends GraphParrotModel implements ParrotModelWithHistory {

	private static final Logger LOG = Logger.getLogger(GraphParrotModelWithHistory.class);
	
	private DirectedSparseMultigraph<NodeWrapper, NodeWrapper> delegateGraph;
	private TreeMap<Version, OntModel> versionedOntModels = new TreeMap<Version, OntModel>();
	
	private Map<OntResource, VersionedNodeWrapper> nodes;
	private Map<OntProperty, Map<Statement, VersionedNodeWrapper>> edges;
	private Map<OntResource, NodeWrapper> types;
	private Map<Literal, Map<Statement, VersionedNodeWrapper>> literals;
	private HashSet<NodeWrapper> typesForIndividual;
	
	private EventListenerList modelListeners;
	
	private HashSet<Filter> restrictFilters;
	private Filter highlightFilter;
	private EnumMap<ChangeType, Filter> versionFilter = new EnumMap<ChangeType, Filter>(ChangeType.class);
	
	private TextSearchEngine textSearchSupport;
	
	private ArrayList<NodeWrapper> currentlyRestricted;
	
	private HistoryModel historyModel;
	private OntModel ontModel;
	private OntModel cOntModel;
	
	private boolean busy;
	
	private IntervalChain<NodeWrapper> timedThings;
	private QuadTree<CenteredThing<NodeWrapper>> locatedThings;
	
//	private Set<NodeWrapper> subjects;
	private Set<NodeWrapper> subjectTypes;
	
	private String datafile;
	
	
	public GraphParrotModelWithHistory(OntModel ontModel) {
		super(ontModel);
		LOG.setLevel(Level.ALL);
		LOG.debug("Create GraphParrotModelWithHistory.");
		this.ontModel = ontModel;
		delegateGraph = new DirectedSparseMultigraph<NodeWrapper, NodeWrapper>();
	}

	public Graph<NodeWrapper, NodeWrapper> asGraph() {
		return delegateGraph;
	}

	
	/* ParrotModel methods */


	@Override
	public void saveData() {
		// TODO #13
		throw new IllegalStateException("Not implemented.");
	}

	@Override
	public OntModel getOntModel() {
		return ontModel;
	}

	@Override
	public Set<NodeWrapper> getSubjectTypes() {
		// TODO Auto-generated method stub
		if (subjectTypes == null) {
			subjectTypes = new HashSet<NodeWrapper>();
			for (ExtendedIterator<OntClass> namedClasses = ontModel.listNamedClasses(); namedClasses.hasNext();) {
				OntClass ontClass = namedClasses.next();
				if (ParrotModelHelper.showTypeAsPrimary(ontModel, ontClass)) {
					NodeWrapper classWrapper;
					if (types.containsKey(ontClass)) {
						classWrapper = types.get(ontClass);
					} else {
						classWrapper = new NodeWrapper(ontClass);
						types.put(ontClass, classWrapper);
					}
					subjectTypes.add(classWrapper);
				}
			}	
		}
		return subjectTypes;
	}

//	@Override
//	public Set<NodeWrapper> getAllSubjects() {
//		// TODO Auto-generated method stub
//		if (subjects == null) {
//			Logger.getLogger(GraphParrotModelWithHistory.class).error("method \"getAllSubjects()\" is not correctly implemented"); 
//			Set<NodeWrapper> subjects = new HashSet<NodeWrapper>();
//			for (ResIterator iterator = cOntModel.listSubjects(); iterator.hasNext();) {
//				Resource subRes = (Resource) iterator.next();
//				if (!subRes.canAs(OntResource.class)) {
//					continue;
//				}
//				OntResource sub = cOntModel.getOntResource(subRes);
//				if (nodes.containsKey(sub)) {
//					subjects.add(nodes.get(sub));
//				}
//			}
//		}
//		return subjects;
//	}

	@Override
	public Set<NodeWrapper> getTypesForIndividual(NodeWrapper node) {
		if (typesForIndividual == null) {
			typesForIndividual = new HashSet<NodeWrapper>();
			if (node.isOntResource() && node.getOntResource().isIndividual()) {
				Individual ind = node.getOntResource().asIndividual();
				ExtendedIterator<OntClass> classes = ind.listOntClasses(false);
				while (classes.hasNext()) {
					OntClass ontClass = classes.next();
					if (ParrotModelHelper.showTypeAsPrimary(ontModel, ontClass) 
							|| ParrotModelHelper.showTypeAsSecondary(ontModel, ontClass)) {
						NodeWrapper classWrapper;
						if (types.containsKey(ontClass)) {
							classWrapper = types.get(ontClass);
						} else {
							classWrapper = new NodeWrapper(ontClass);
							types.put(ontClass, classWrapper);
						}
						typesForIndividual.add(classWrapper);
					}
				}
			}
		}
		return typesForIndividual;
	}

	@Override
	public Set<NodeWrapper> getIndividualsForType(NodeWrapper type) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (type.isType() && type.getOntResource().isClass()) {
			OntClass ontClass = type.getOntResource().asClass();
			ExtendedIterator<? extends OntResource> individuals = ontClass.listInstances(false);
			while (individuals.hasNext()) {
				OntResource individual = individuals.next();
				if (individual.isIndividual()) {
					result.add(getNodeWrapper(individual.asIndividual()));
				}
			}
		}
		return result;
	}

	@Override
	public Set<NodeWrapper> getSuperPredicates(NodeWrapper node) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (node.isOntResource() && node.getOntResource().isProperty()) {
			OntProperty prop = node.getOntResource().asProperty();
			ExtendedIterator<? extends OntProperty> superProps = prop.listSuperProperties(false);
			while (superProps.hasNext()) {
				OntProperty superProp = superProps.next();
				if (ParrotModelHelper.showTypeAsPrimary(ontModel, superProp) 
						|| ParrotModelHelper.showTypeAsSecondary(ontModel, superProp)) {
					result.add(new NodeWrapper(superProp));
				}
			}
		}
		return result;
	}

	@Override
	public void addFilter(Filter filter) {
		if (!busy) {
			busy = true;
			fireBusyChanged(true);
		}
		if (filter.getMode() == Filter.Mode.HIGHLIGHT) {
			// there is only one highlight filter
			Object oldHighlight = highlightFilter;
			if (oldHighlight != filter) {
				highlightFilter = filter;
				updateHighlights();
				fireHighlightsChanged();
			}
		} else if (filter.getMode() == Filter.Mode.RESTRICT) {
			boolean added = restrictFilters.add(filter);
			if (added) {
				currentlyRestricted.addAll(filter.getMatching(this));
				List<NodeWrapper> newRestricted = new ArrayList<NodeWrapper>(currentlyRestricted);
				fireRestrictionsChanged(newRestricted);
			}
		} else if (filter.getMode() == Filter.Mode.VERSION) {
			// there are exactly three version filter
			ChangeType type = ((VersionFilter) filter).getType();
			Filter oldVersion = versionFilter.get(type);
			if (oldVersion != filter) {
				versionFilter.put(type, filter);
				updateVersion(((VersionFilter) filter).getVersion(), type);
				fireVersionChanged();
			}
		}
		if (busy) {
			busy = false;
			fireBusyChanged(false);
		}
	}

	@Override
	public void removeFilter(Filter filter) {
		if (!busy) {
			busy = true;
			fireBusyChanged(true);
		}
		// TODO #21 other filter types
		if (filter.getMode() == Filter.Mode.HIGHLIGHT) {
			// there is only one highlight filter
			highlightFilter = null;
			// update highlight information in nodewrappers
			updateHighlights();
			fireHighlightsChanged();
		} else if (filter.getMode() == Filter.Mode.RESTRICT) {
			boolean removed = restrictFilters.remove(filter);
			if (removed) {
				currentlyRestricted.clear();
				for (Filter theFilter : restrictFilters) {
					currentlyRestricted.addAll(theFilter.getMatching(this));
				}
				List<NodeWrapper> newRestricted = new ArrayList<NodeWrapper>(currentlyRestricted);
				fireRestrictionsChanged(newRestricted);
			}
		} else if (filter.getMode() == Filter.Mode.VERSION) {
			// there are exactly three version filter
			ChangeType type = ((VersionFilter) filter).getType();
			versionFilter.remove(type);
			updateVersion(null, type);
			fireVersionChanged();
		}
		if (busy) {
			busy = false;
			fireBusyChanged(false);
		}
	}

	@Override
	public void replaceFilter(Filter oldFilter, Filter newFilter) {
		if (oldFilter == null && newFilter == null) {
			// do nothing
			return;
		}
		if (oldFilter == null) {
			addFilter(newFilter);
			return;
		} else if (newFilter == null) {
			removeFilter(oldFilter);
			return;
		}
		if (oldFilter.getMode() != newFilter.getMode()) {
			Logger logger = Logger.getLogger(GraphParrotModelWithHistory.class);
			logger.warn("trying to replace a filter with one with a different mode, aborting");
			// do nothing
			return;
		}
		// ok, now we know we are genuinely replacing filters
		if (!busy) {
			busy = true;
			fireBusyChanged(true);
		}
		// TODO #21 other filter types
		if (newFilter.getMode() == Filter.Mode.HIGHLIGHT) {
			// there is only one highlight filter
			if (highlightFilter != oldFilter) {
				Logger logger = Logger.getLogger(GraphParrotModelWithHistory.class);
				logger.warn("trying to replace filters, but old filter doesn't exist. Updating the filter anyway.");
			}
			highlightFilter = newFilter;
			updateHighlights();
			fireHighlightsChanged();
		} else if (newFilter.getMode() == Filter.Mode.RESTRICT) {
			boolean removed = restrictFilters.remove(oldFilter);
			boolean added = restrictFilters.add(newFilter);
			if (removed || added) {
				currentlyRestricted.clear();
				for (Filter theFilter : restrictFilters) {
					currentlyRestricted.addAll(theFilter.getMatching(this));
				}
				List<NodeWrapper> newRestricted = new ArrayList<NodeWrapper>(currentlyRestricted);
				fireRestrictionsChanged(newRestricted);
			}
		} else if (newFilter.getMode() == Filter.Mode.VERSION) {
			// there are exactly three version filter
			if (((VersionFilter) oldFilter).getType() != ((VersionFilter) newFilter).getType()) {
				Logger logger = Logger.getLogger(GraphParrotModelWithHistory.class);
				logger.warn("trying to replace a filter with one with a different type, aborting");
				// do nothing
				return;
			}
			ChangeType type = ((VersionFilter) oldFilter).getType();
			if (versionFilter.get(type) != oldFilter) {
				Logger logger = Logger.getLogger(GraphParrotModelWithHistory.class);
				logger.warn("trying to replace filters, but old filter doesn't exist. Updating the filter anyway.");
			}
			versionFilter.put(type, newFilter);
			updateVersion(((VersionFilter) newFilter).getVersion(), type);
			fireVersionChanged();

		}
		if (busy) {
			busy = false;
			fireBusyChanged(false);
		}
	}

	@Override
	public NodeWrapper getNodeWrapper(Individual instance) {
		if (nodes.containsKey(instance)) {
			return nodes.get(instance);
		} else {
			Logger logger = Logger.getLogger(GraphParrotModelWithHistory.class);
			logger.warn("don't have a wrapper for " + instance);
			// TODO does this cause problems? YES
			return null;
		}
	}

	@Override
	public Set<NodeWrapper> getNodeWrappers(OntClass ontClass) {
		if (edges.containsKey(ontClass)) {
			return new HashSet<NodeWrapper>(edges.get(ontClass).values());
		} else {
			Logger logger = Logger.getLogger(GraphParrotModelWithHistory.class);
			logger.warn("don't have wrappers for " + ontClass);
			// TODO does this cause problems? YES
			return null;
		}
	}

	@Override
	public Set<NodeWrapper> getAllNodeWrappers() {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		result.addAll(nodes.values());
		for (OntResource edgeResource : edges.keySet()) {
			result.addAll(edges.get(edgeResource).values());
		}
		return result;
	}

	@Override
	public Set<NodeWrapper> getAllNodes() {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>(nodes.values().size());
		result.addAll(nodes.values());
		return result;
	}

	@Override
	public IntervalChain<NodeWrapper> getTimedThings() {
		if (timedThings == null) {
			timedThings = TimedThingsHelper.extractTimedThings(this);
		}
		return timedThings;
	}

	@Override
	public QuadTree<CenteredThing<NodeWrapper>> getLocatedThings() {
		if (locatedThings == null) {
			locatedThings = LocatedThingsHelper.extractLocatedThings(this);
		}
		return locatedThings;
	}

	@Override
	public void addParrotModelListener(ParrotModelListener pml) {
		modelListeners.add(ParrotModelListener.class, pml);
	}

	@Override
	public void removeParrotModelListener(ParrotModelListener pml) {
		modelListeners.remove(ParrotModelListener.class, pml);
	}

	@Override
	public Set<NodeWrapper> searchNodeWrappers(String query) throws SearchFailedException {
		return textSearchSupport.search(query);
	}

	@Override
	public boolean isBusy() {
		return busy;
	}

	@Override
	public Set<NodeWrapper> getNodeWrappersOnChain(List<ChainLink> chain) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();

		if (chain.isEmpty()) {
			// chain is empty, done
			return result;
		}
		
		if (chain.size() == 1) {
			ChainLink firstInChain = chain.get(0);
			if (!firstInChain.hasType() && !firstInChain.hasInstance()) {
				result.addAll(nodes.values());
				for (Map<Statement, VersionedNodeWrapper> edgeMap : edges.values()) {
					result.addAll(edgeMap.values());
				}
				return result;
			}
		}
		
		List<List<NodeWrapper>> nodeChains = getChains(chain);

		// now that potentialNodeChains only has NodeWrappers remaining that
		// actually match the chain, we can go and add them all to the result
		// set
		// but we need to make sure we add the edges along the chains too
		for (List<NodeWrapper> nodeChain : nodeChains) {
			Set<NodeWrapper> chainWithEdges = makeNodeChainWithEdges(nodeChain);
			result.addAll(chainWithEdges);
		}
		return result;
	}

	@Override
	public List<List<NodeWrapper>> getChains(List<ChainLink> chain) {
		// create data structure to hold all potentially matching NodeWrappers
		// it's a list of lists of matching NodeWrappers (each "inner" list
		// mirroring the chain up to the current iteration step)
		List<List<NodeWrapper>> potentialNodeChains = new ArrayList<List<NodeWrapper>>();

		// first step: initialise the inner lists (one for each starting point)
		ChainLink firstLink = chain.get(0);
		if (firstLink.hasInstance()) {
			// there is only one starting point
			List<NodeWrapper> potentialNodeChain = new ArrayList<NodeWrapper>(chain.size());
			potentialNodeChain.add(firstLink.getInstance());
			potentialNodeChains.add(potentialNodeChain);
		} else if (firstLink.hasType()) {
			// every instance of firstLink's type is a starting point
			Set<NodeWrapper> instances = getIndividualsForType(firstLink.getType());
			for (NodeWrapper instance : instances) {
				List<NodeWrapper> potentialNodeChain = new ArrayList<NodeWrapper>(chain.size());
				potentialNodeChain.add(instance);
				potentialNodeChains.add(potentialNodeChain);
			}
		} else {
			// firstLink is any/any type 
			// this means *all* subjects match
			List<NodeWrapper> allSubjects = new ArrayList<NodeWrapper>(nodes.values());
			potentialNodeChains.add(allSubjects);
			return potentialNodeChains;
		}

		// now that we're done initialising potentialNodeChains, go and iterate
		// along the chain (starting at the *second* item since we've already
		// looked at the first one)
		for (int i = 1; i < chain.size(); i++) {
			ChainLink link = chain.get(i);
			
			List<List<NodeWrapper>> keepChains = new ArrayList<List<NodeWrapper>>();
			List<List<NodeWrapper>> newChains = new ArrayList<List<NodeWrapper>>();
			
			if (link.hasInstance()) {
				NodeWrapper instance = link.getInstance();
				// keep all potentialNodeChains whose end is one step before instance
				for (List<NodeWrapper> potentialNodeChain : potentialNodeChains) {
					NodeWrapper endOfChain = potentialNodeChain.get(potentialNodeChain.size() - 1);
					// check whether we can get from endOfChain to instance
					if (hasSuccessor(endOfChain, instance)) {
						// yup -> keep this potentialNodeChain
						potentialNodeChain.add(instance);
						keepChains.add(potentialNodeChain);
					}
				}
			} else if (link.hasType()) {
				Set<NodeWrapper> instances = getIndividualsForType(link.getType());
				// keep all potentialNodeChains whose end is one step before *one of* the instances
				for (List<NodeWrapper> potentialNodeChain : potentialNodeChains) {
					NodeWrapper endOfChain = potentialNodeChain.get(potentialNodeChain.size() - 1);
					
					// endOfChain could have *several* of the instances as successors
					// in that case we need to branch out
					boolean alreadyFoundOne = false;
					for (NodeWrapper instance : instances) {
						if (hasSuccessor(endOfChain, instance)) {
							if (!alreadyFoundOne) {
								// the easy case: this is the first time we're finding a match
								potentialNodeChain.add(instance);
								keepChains.add(potentialNodeChain);
								alreadyFoundOne = true;
							} else {
								// the tricky case: we have already found a different match before
								// we need to copy the whole potentialNodeChain
								List<NodeWrapper> newPotentialNodeChain = new ArrayList<NodeWrapper>(chain.size());
								// need to add instance separately because it's a different one
								newPotentialNodeChain.addAll(potentialNodeChain.subList(0, potentialNodeChain.size() - 1));
								newPotentialNodeChain.add(instance);
								// and add the copy to newChains
								newChains.add(newPotentialNodeChain);
							}
						}
					}
				}
			} else {
				// link is any/any type 
				// -> keep all potentialNodeChains whose end has at least one outgoing edge
				// while doing the same branchy stuff as in the 'else if' case above
				for (List<NodeWrapper> potentialNodeChain : potentialNodeChains) {
					NodeWrapper endOfChain = potentialNodeChain.get(potentialNodeChain.size() - 1);
					Collection<NodeWrapper> successors = getSuccessorNodes(endOfChain);
					for (NodeWrapper successor : successors) {
						List<NodeWrapper> newPotentialNodeChain = new ArrayList<NodeWrapper>();
						newPotentialNodeChain.addAll(potentialNodeChain);
						newPotentialNodeChain.add(successor);
						newChains.add(newPotentialNodeChain);
					}
				}
			}

			// only keep those potentialNodeChains that 'survived' this iteration
			potentialNodeChains.retainAll(keepChains);
			// and add the new ones
			potentialNodeChains.addAll(newChains);
		}
		return potentialNodeChains;
	}

	@Override
	public boolean hasSuccessor(NodeWrapper node, NodeWrapper maybeSuccessor) {
		return delegateGraph.isSuccessor(maybeSuccessor, node);
	}

	@Override
	public Collection<NodeWrapper> getSuccessorNodes(NodeWrapper node) {
		return delegateGraph.getSuccessors(node);
	}

	@Override
	public Collection<NodeWrapper> getEdges(NodeWrapper from, NodeWrapper to) {
		return new ArrayList<NodeWrapper>(delegateGraph.getEdges());
	}

	@Override
	public NodeWrapper getNodeWrapperForString(String url) throws NoSuchNodeWrapperException {
		// TODO use appropriate model! (needed for map navigator)
		Logger.getLogger(GraphParrotModelWithHistory.class).error("method \"getNodeWrapperForString(String url)\" is not correctly implemented"); 
		Individual individual = cOntModel.getIndividual(url);
		if (individual == null) {
			throw new NoSuchNodeWrapperException("no nodewrapper found for string " + url);
		}
		return getNodeWrapper(individual);
	}

	@Override
	public String getDataIdentifier() {
		return datafile;
	}

	@Override
	public GraphParrotModel asGraphModel() {
		return this;
	}

	@Override
	public TableParrotModel asListModel() {
		return null;
	}

	@Override
	public void loadData(String datafile) {
		throw new IllegalAccessError("Use this loadData methode only with both arguments datafile and historyfile.");
	}

	@Override
	public void loadData(String datafile, String historyfile) {
		LOG.debug("Load data from datafile (" + datafile + ") and historyfile (" + historyfile + ").");
		this.datafile = datafile;
		
		nodes = new HashMap<OntResource, VersionedNodeWrapper>();
		edges = new HashMap<OntProperty, Map<Statement,VersionedNodeWrapper>>();
		types = new HashMap<OntResource, NodeWrapper>();
		literals = new HashMap<Literal, Map<Statement,VersionedNodeWrapper>>();
		
		modelListeners = new EventListenerList();

		restrictFilters = new HashSet<Filter>();
		textSearchSupport = new TextSearchEngine();
		currentlyRestricted = new ArrayList<NodeWrapper>();
		
		LOG.debug("... read ontology model from datafile");
		ontModel.read(datafile);
		ontModel.rebind();
		
		((PelletInfGraph) ontModel.getGraph()).classify();
		((PelletInfGraph) ontModel.getGraph()).realize();
		
		LOG.debug("... read history model from historyfile");
		historyModel = HistoryModel.load(historyfile);
		versionedOntModels = historyModel.getVersionedOntModels(ontModel);
		
		LOG.debug("... populate the graph with nodes and edges");
		// populate graph
		
		for (Map.Entry<Version, OntModel> versionedOntModel : versionedOntModels.entrySet()) {
			Version version = versionedOntModel.getKey();
			OntModel model = versionedOntModel.getValue();
			
			ModelExtractor extractor = new ModelExtractor(model);
			EnumSet<StatementType> selectors = StatementType.PROPERTY_VALUE;
			extractor.setSelector(selectors);
			Model eModel = extractor.extractModel();
			
			StmtIterator statements = eModel.listStatements();
			while (statements.hasNext()) {
				Statement stat = statements.nextStatement();

				Resource subject = stat.getSubject();
				if (subject.isURIResource())
					subject = model.getResource(subject.getURI());
				if (!subject.canAs(Individual.class) || !ParrotModelHelper.isPotentialNode(model, subject.as(Individual.class)))
					continue;
				
				Property predicate = stat.getPredicate();
				if (predicate.isURIResource())
					predicate = model.getProperty(predicate.getURI());
				if (!ParrotModelHelper.isPotentialEdge(model, predicate))
					continue;
				
				RDFNode object = stat.getObject();
				if (object.isURIResource())
					object = model.getResource(object.as(Resource.class).getURI());
				
				
				// add subject to node list
				VersionedNodeWrapper subjectWrapper = null;
				if (nodes.containsKey(subject.as(Individual.class))) {
					subjectWrapper = nodes.get(subject.as(Individual.class)).addResourceVersion(version, subject.as(Individual.class));
					// TODO textSearchSupport.update(subjectWrapper);
				} else {
					subjectWrapper = new VersionedNodeWrapper(historyModel, version, subject.as(Individual.class));
					nodes.put(subject.as(Individual.class), subjectWrapper);
					textSearchSupport.add(subjectWrapper);
				}
				
				
				// add Object to node or literal list
				VersionedNodeWrapper objectWrapper = null;
				if (object.isURIResource()) {
					OntResource objectResource = model.getOntResource(object.as(Resource.class));
					if (nodes.containsKey(objectResource)) {
						objectWrapper = nodes.get(objectResource).addResourceVersion(version, objectResource);
						// TODO textSearchSupport.update(objectWrapper);
					} else {
						objectWrapper = new VersionedNodeWrapper(historyModel, version, objectResource);
						nodes.put(objectResource, objectWrapper);
						textSearchSupport.add(objectWrapper);
					}
				} else if (object.isLiteral()) {
					if (!literals.containsKey(object.asLiteral()))
						literals.put(object.asLiteral(), new HashMap<Statement, VersionedNodeWrapper>());
					Map<Statement, VersionedNodeWrapper> entryMap = literals.get(object.asLiteral());
					if (entryMap != null && entryMap.containsKey(stat)) {
						objectWrapper = entryMap.get(stat).addLiteralVersion(version, object.asLiteral());
						// TODO textSearchSupport.update(objectWrapper);
					} else {
						objectWrapper = new VersionedNodeWrapper(historyModel, version, object.asLiteral());
						literals.get(object.asLiteral()).put(stat, objectWrapper);
						textSearchSupport.add(objectWrapper);
					}
				} else
					throw new IllegalStateException("Can only handle literals or URI resources as objects.");
				
				// add predicate to edge list
				OntProperty predicateProperty = model.getOntResource(predicate).asProperty();
				VersionedNodeWrapper predicateWrapper = null;
				
				if (!edges.containsKey(predicateProperty))
					edges.put(predicateProperty, new HashMap<Statement, VersionedNodeWrapper>());
				Map<Statement, VersionedNodeWrapper> entryMap = edges.get(predicateProperty);
				if (entryMap != null && entryMap.containsKey(stat)) {
					predicateWrapper = entryMap.get(stat).addVersion(version);
					// TODO textSearchSupport.update(objectWrapper);
				} else {
					predicateWrapper = new VersionedNodeWrapper(historyModel, version, predicateProperty);
					edges.get(predicateProperty).put(stat, predicateWrapper);
					textSearchSupport.add(predicateWrapper);
				}
				
				// add to graph
				delegateGraph.addVertex(subjectWrapper);
				if (objectWrapper != null) {
					delegateGraph.addVertex(objectWrapper);
					delegateGraph.addEdge(predicateWrapper, subjectWrapper, objectWrapper);
				}
			}

		}
	}

	@Override
	public HistoryModel getHistoryModel() {
		return historyModel;
	}

	
	/* private methods from Parrot Helper */
	
	private void fireBusyChanged(final boolean nowBusy) {
		// from EventListenerList javadocs (but minus the bugs)
		final ParrotModelListener[] listeners = modelListeners.getListeners(ParrotModelListener.class);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Process the listeners last to first, notifying
				// those that are interested in this event
				for (int i = listeners.length - 1; i >= 0; i--) {
					if (nowBusy) {
						listeners[i].modelBusy();
					} else {
						listeners[i].modelIdle();
					}
				}
			}
		});
	}
	
	private void fireRestrictionsChanged(final Collection<NodeWrapper> newRestricted) {
		// from EventListenerList javadocs (but minus the bugs)
		final ParrotModelListener[] listeners = modelListeners.getListeners(ParrotModelListener.class);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Process the listeners last to first, notifying
				// those that are interested in this event
				for (int i = listeners.length - 1; i >= 0; i--) {
					listeners[i].restrictionsChanged(newRestricted);
				}
			}
		});
	}
	
	private void fireHighlightsChanged() {
		// from EventListenerList javadocs (but minus the bugs)
		final ParrotModelListener[] listeners = modelListeners.getListeners(ParrotModelListener.class);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Process the listeners last to first, notifying
				// those that are interested in this event
				for (int i = listeners.length - 1; i >= 0; i--) {
					listeners[i].highlightsChanged();
				}
			}
		});
	}
	
	private void updateHighlights() {
		// first reset all highlights
		List<NodeWrapper> allNodeWrappers = new ArrayList<NodeWrapper>();
		allNodeWrappers.addAll(nodes.values());
		for (OntResource key : edges.keySet()) {
			allNodeWrappers.addAll(edges.get(key).values());
		}
		for (NodeWrapper nodeWrapper : allNodeWrappers) {
			nodeWrapper.setHighlighted(false);
		}
		// now highlight those that should be, if we have a highlight filter
		if (highlightFilter == null) {
			return;
		}
		Set<NodeWrapper> matching = highlightFilter.getMatching(this);
		for (NodeWrapper nodeWrapper : matching) {
			nodeWrapper.setHighlighted(true);
		}
	}
	
	private void fireVersionChanged() {
		// from EventListenerList javadocs (but minus the bugs)
		final ParrotModelListener[] listeners = modelListeners.getListeners(ParrotModelListener.class);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				// Process the listeners last to first, notifying
				// those that are interested in this event
				for (int i = listeners.length - 1; i >= 0; i--) {
					listeners[i].versionChanged();
				}
			}
		});
	}
	
	private void updateVersion(Version version, ChangeType type) {
		List<VersionedNodeWrapper> allNodeWrappers = new ArrayList<VersionedNodeWrapper>();
		allNodeWrappers.addAll(nodes.values());
		for (OntResource key : edges.keySet())
			allNodeWrappers.addAll(edges.get(key).values());
		// TODO only process matching wrappers? 
		for (VersionedNodeWrapper nodeWrapper : allNodeWrappers)
			nodeWrapper.setActiveVersion(version);
	}
	
	private Set<NodeWrapper> makeNodeChainWithEdges(List<NodeWrapper> chain) {
		Set<NodeWrapper> result = new HashSet<NodeWrapper>();
		if (chain.isEmpty()) {
			return result;
		}
		NodeWrapper lastNode = null;
		for (NodeWrapper node : chain) {
			if (lastNode == null) {
				// looking at first in chain
				lastNode = chain.get(0);
			} else {
				result.addAll(getEdges(lastNode, node));
				lastNode = node;
			}
			result.add(lastNode);
		}
		return result;
	}

	@Override
	public TreeMap<Version, OntModel> getVersionedOntModels() {
		return versionedOntModels;
	}

}
