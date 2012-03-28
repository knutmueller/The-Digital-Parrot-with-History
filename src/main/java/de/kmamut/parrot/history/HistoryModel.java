/*
 * Copyright (C) 2012 Knut Müller
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


package de.kmamut.parrot.history;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.mindswap.pellet.jena.PelletInfGraph;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.kmamut.parrot.history.changes.Change;
import de.kmamut.parrot.history.changes.ChangeSet;
import de.kmamut.parrot.history.changes.Version;

/**
 * Aggregates several settings which the digital parrot's ontologies have in common.  
 * @author Knut Müller
 */
public class HistoryModel {
	
	private final Logger LOG = Logger.getLogger(HistoryModel.class);
	
	// initialize HistoryModel
	
	private OntModel historyModel = HistoryModelHelper.createHistoryModel();
	private File historyFile = null;
	private TreeSet<ChangeSet> changeSets = null;
	private TreeMap<Version, OntModel> versionedOntModels = null;
	
	// create HistoryModel and load data from file
	
	/**
	 * Create new HistoryModel and read history data from file.
	 * @param filename Name of history data file.
	 * @return A new HistoryModel with history data.
	 */
	public static HistoryModel load(String filename) {
		return new HistoryModel(filename);
	}
	
	// constructor; construct HistoryModel with method "HistoryModel load(String filename)"
	private HistoryModel(String filename) {
		LOG.setLevel(Level.ALL);
		filename = filename.replaceFirst("^file:", "");
		historyFile = new File(filename);
		if (historyFile.exists()) {
			historyModel.read("file:" + historyFile.getAbsolutePath());
			historyModel.rebind();
			changeSets = HistoryModelHelper.loadChangeSets(historyModel);
		} else
			LOG.info("History file does not exist: " + historyFile.getAbsolutePath());
	}
	
	// update HistoryModel and write back to file
	
	/**
	 * Add changes from changeSets to history model.
	 * @param changeSets A set of ChangeSet sorted by version.
	 * @return This (updated) HistoryModel.
	 */
	public HistoryModel update(TreeSet<ChangeSet> changeSets) {
		HistoryModelHelper.updateChangeSets(historyModel, changeSets);
		changeSets.addAll(changeSets);
		return this;
	}

	/**
	 * Writes itself to given file. Creates the file if it does not exist. 
	 * @return This HistoryModel that was written
	 */
	public HistoryModel write() {
		try {
			boolean hasOutdatedHistory = historyFile.exists();
			FileOutputStream stream = new FileOutputStream(historyFile);
			historyModel.write(stream);
			LOG.info("Sucessfully " + (hasOutdatedHistory ? "updated" : "created") + " history ontology.");
			stream.close();
		} catch (FileNotFoundException e) {
			LOG.error("Can't write history to history file: " + e.getMessage());
		} catch (IOException e) {
			LOG.error("Problems finishing writing to history file: " + e.getMessage());
		}
		return this;
	}
	
	// condition data from history model for use in digital parrot
	// build ontology models and provide version information
	
	/**
	 * Create and return available OntModels from history model, sorted by Version.
	 * @param lastOntModel the ontology model of the latest Version in history model. 
	 * 	Prior Versions are constructed from history model and this ontology model. 
	 * @return TreeMap with versions as keys and ontology models as values.  
	 */
	public TreeMap<Version, OntModel> getVersionedOntModels(OntModel lastOntModel) {
		if (versionedOntModels == null) {
			versionedOntModels = new TreeMap<Version, OntModel>();
			LOG.debug("... add last ontology model (" + changeSets.last().getVersion() + ") to versioned ontology models");
			versionedOntModels.put(changeSets.last().getVersion(), lastOntModel);
			for (ChangeSet changeSet : changeSets.descendingSet()) {
				OntModel ontModel = versionedOntModels.get(changeSet.getVersion());
				if (changeSet.hasPriorVersion()) {
					LOG.debug("... create prior ontology model (" + changeSet.getPriorVersion() + ")");
					versionedOntModels.put(changeSet.getPriorVersion(), createPriorOntModel(ontModel, changeSet));
				}
			}
		}
		return versionedOntModels;
	}
	
	private OntModel createPriorOntModel(OntModel model, ChangeSet changeSet) {
		OntModel priorModel = HistoryModelHelper.createEmptyParrotModel();
		priorModel.add(model.getBaseModel());
		priorModel.loadImports();
		
		Set<Change> allAdditions = new HashSet<Change>(changeSet.getAdditions());
		allAdditions.addAll(changeSet.getChanges());
		Set<Change> allRemovals = new HashSet<Change>(changeSet.getRemovals());
		allRemovals.addAll(changeSet.getChanges());
		
		// set added statements in addition changes and change changes
		for (Change change : allAdditions) {
			Individual subject = priorModel.getIndividual(change.getSubjectURI());
			Property predicate = priorModel.getProperty(change.getPredicateURI());
			for (Statement statement : subject.listProperties(predicate).toSet()) {
				String objectString = change.getNewObjectValue();
				RDFNode object = statement.getObject();
				if ((object.isLiteral() && object.asLiteral().getLexicalForm().equals(objectString)) || 
						(object.isURIResource() && object.asResource().getURI().equals(objectString))) {
					change.setNewStatement(model.listStatements(subject, predicate, object).nextStatement());
					break;
				}
			}
		}
		
		// remove additions from the prior model
		for (Change change : allAdditions) {
			Individual subject = priorModel.getIndividual(change.getSubjectURI());
			Property predicate = priorModel.getProperty(change.getPredicateURI());
			if (subject == null)
				continue;
			if (predicate.equals(RDF.type)) {
				subject.remove();
				continue;
			}
			if (predicate.equals(RDFS.label)) {
				subject.removeLabel(change.getNewObjectValue(), change.getLanguage());
				continue;
			}
			for (RDFNode object : subject.listPropertyValues(predicate).toSet()) {
				String objectString = change.getNewObjectValue();
				if ((object.isLiteral() && object.asLiteral().getLexicalForm().equals(objectString)) || 
						(object.isURIResource() && object.asResource().getURI().equals(objectString))) {
					subject.removeProperty(predicate, object);
					break;
				}
			}
		}
		
		// add removals to prior model
		for (Change change : allRemovals) {
			Property predicate = priorModel.getProperty(change.getPredicateURI());
			if (predicate.equals(RDF.type)) {
				priorModel.createIndividual(change.getSubjectURI(), model.getOntClass(change.getOldObjectValue()));
			}
		}
		for (Change change : allRemovals) {
			Property predicate = priorModel.getProperty(change.getPredicateURI());
			if (predicate.equals(RDF.type))
				continue;
			Individual subject = priorModel.getIndividual(change.getSubjectURI());
			if (predicate.equals(RDFS.label)) {
				subject.setLabel(change.getOldObjectValue(), change.getLanguage());
				continue;
			}
			
			String objectString = change.getOldObjectValue();
			if (change.isLiteralChange()) {
				if (change.isLabelChange()) {
					String language = change.getLanguage();
					subject.setLabel(objectString, language);
				} else {
					RDFDatatype datatype = TypeMapper.getInstance().getSafeTypeByName(change.getDatatypeURI());
					subject.addProperty(predicate, objectString, datatype);
				}
			} else {
				OntResource object = priorModel.getOntResource(objectString);
				if (object == null)
					object = priorModel.createOntResource(objectString);
				subject.addProperty(predicate, object);
			}
		}
		
		// set removed statements in removal changes and change changes
		for (Change change : allRemovals) {
			Individual subject = priorModel.getIndividual(change.getSubjectURI());
			Property predicate = priorModel.getProperty(change.getPredicateURI());
			if (predicate.equals(RDFS.label)) {
				change.setOldStatement(priorModel.listStatements(subject, predicate, change.getOldObjectValue(), change.getLanguage()).nextStatement());
				continue;
			}
			for (Statement statement : subject.listProperties(predicate).toSet()) {
				String objectString = change.getOldObjectValue();
				RDFNode object = statement.getObject();
				if ((object.isLiteral() && object.asLiteral().getLexicalForm().equals(objectString)) || 
						(object.isURIResource() && object.asResource().getURI().equals(objectString))) {
					change.setOldStatement(priorModel.listStatements(subject, predicate, object).nextStatement());
					break;
				}
			}
		}
		
		// reason new ontology data
		priorModel.rebind();
		((PelletInfGraph) priorModel.getGraph()).classify();
		((PelletInfGraph) priorModel.getGraph()).realize();
		return priorModel;
	}

	/**
	 * Get in this history model available versions. 
	 * @return List of Versions, sorted in chronological order.
	 */
	public List<Version> getVersions() {
		if (versionedOntModels == null)
			return new ArrayList<Version>();
		return new ArrayList<Version>(versionedOntModels.keySet());
	}
	
}
