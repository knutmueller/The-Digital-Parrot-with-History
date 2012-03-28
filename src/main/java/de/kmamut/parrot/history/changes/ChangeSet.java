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


package de.kmamut.parrot.history.changes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import de.kmamut.parrot.history.OntologyModelSettings;

public class ChangeSet implements Versioned<ChangeSet> {
	
	private static final Logger LOG = Logger.getLogger(ChangeSet.class);
	
	private Map<ChangeType, Set<Change>> changeSetsByType = initializeChangeSetsByTypeMap();
	private static Map<ChangeType, Set<Change>> initializeChangeSetsByTypeMap() {
		HashMap<ChangeType, Set<Change>> changeSetsByType = new HashMap<ChangeType, Set<Change>>(ChangeType.values().length);
		for (ChangeType type : ChangeType.values())
			changeSetsByType.put(type, new HashSet<Change>());
		return changeSetsByType;
	}
	
	private Map<ChangeType, Map<String, Set<Change>>> changesByTypeAndSubjectURI = initializeChangesByTypeAndSubjectURIMap();
	private static Map<ChangeType, Map<String, Set<Change>>> initializeChangesByTypeAndSubjectURIMap() {
		HashMap<ChangeType, Map<String, Set<Change>>> changesByTypeAndSubjectURI = new HashMap<ChangeType, Map<String, Set<Change>>>(ChangeType.values().length);
		for (ChangeType type : ChangeType.values())
			changesByTypeAndSubjectURI.put(type, new HashMap<String, Set<Change>>());
		return changesByTypeAndSubjectURI;
	} 
	
	private Map<ChangeType, Map<String, String>> subjectTypesBySubjectURI = initializeSubjectTypesByTypeAndSubjectURIMap();
	private static Map<ChangeType, Map<String, String>> initializeSubjectTypesByTypeAndSubjectURIMap() {
		Map<ChangeType, Map<String, String>> subjectTypesBySubjectURI = new HashMap<ChangeType, Map<String, String>>(ChangeType.values().length);
		for (ChangeType type : ChangeType.values())
			subjectTypesBySubjectURI.put(type, new HashMap<String, String>());
		return subjectTypesBySubjectURI;
	}

	private static String logTime(long time) {
		return (time / 1000000) + " ms";
	}

	
	private Version version;
	private Version priorVersion;

	/**
	 * Constructs ChangeSet that holds information about added, changed, and removed statements in newOntology compared to oldOntology.
	 * @param oldOntology ComparableOntology that wraps base (old) ontology.
	 * @param newOntology ComparableOntology that wraps updated (new) ontology.
	 */
	public ChangeSet(ComparableOntology oldOntology, ComparableOntology newOntology) {
		this.version = newOntology.getVersion();
		this.priorVersion = oldOntology.getVersion();
		LOG.setLevel(Level.ALL);
		LOG.debug("Create change set form v" + priorVersion  + " to v" + version + " ...");
		compareOntologies(oldOntology, newOntology);
	}
	
	/**
	 * Create an empty ChangeSet container.
	 * @param version The Version of the ontology that this ChangeSet will leads to.
	 * @param priorVersion The Version of the ontology that this ChagneSet comes from.
	 * @return 
	 */
	public static ChangeSet createEmptyChangeSet(Version version, Version priorVersion) {
		return new ChangeSet(version, priorVersion);
	}
	
	private ChangeSet(Version oldVersion, Version newVersion) {
		this.version = newVersion;
		this.priorVersion = oldVersion;
		LOG.setLevel(Level.ALL);
	}

	/**
	 * Batch creation of ChangeSets: ComparableOntologies are compared in order of their versions.
	 * @param comparableOntologies List of ComparableOntologies.
	 * @return List of ChangeSets that represents the Changes between ComparableOntologies.
	 */
	public static List<ChangeSet> compare(List<ComparableOntology> comparableOntologies) {
		Collections.sort(comparableOntologies);
		List<ChangeSet> changeSets = new ArrayList<ChangeSet>(comparableOntologies.size() - 1);
		for (int i = 0; i < (comparableOntologies.size() - 1); i++)
			changeSets.add(new ChangeSet(comparableOntologies.get(i), comparableOntologies.get(i+1)));
		return changeSets;
	}
	
	private void compareOntologies(ComparableOntology oldOntology, ComparableOntology newOntology) {
		LOG.setLevel(Level.INFO);
		LOG.debug("Comparing " + oldOntology.getFilename());
		LOG.debug("with " + newOntology.getFilename() + "...");
		
		// for measuring
		long startTime = 0;
		int additionCount = 0;
		int removalCount = 0;
		int changeCount = 0;
		
		// if oldOntologyFilename is same as newOntologyFilename, return the empty change set
		if (oldOntology.getFilename().equals(newOntology.getFilename()))
			return;
				
		if (LOG.isInfoEnabled()) { startTime = System.nanoTime(); }
		// create ontology models that hold removed and added statements
		OntModel addedStatementModel = OntologyModelSettings.createModel();
		OntModel removedStatementModel = OntologyModelSettings.createModel();

		// copy prefix mapping from original ontologies
		Map<String, String> prefixMap = new HashMap<String, String>();
		prefixMap.putAll(oldOntology.getOntModel().getNsPrefixMap());
		prefixMap.putAll(newOntology.getOntModel().getNsPrefixMap());
		addedStatementModel.setNsPrefixes(prefixMap);
		removedStatementModel.setNsPrefixes(prefixMap);
		
		// calculate difference - remove statements that occur in both ontologies
		// old - new
		removedStatementModel.add(oldOntology.getOntModel().difference(newOntology.getOntModel()));
		// new - old
		addedStatementModel.add(newOntology.getOntModel().difference(oldOntology.getOntModel()));
		LOG.info("Built difference ontologies. " + logTime(System.nanoTime() - startTime));
		
		if (LOG.isInfoEnabled()) { startTime = System.nanoTime(); }
		// remember statements that are neither added nor removed but changed 
		List<Statement> addedStatements = new ArrayList<Statement>();
		List<Statement> removedStatements = new ArrayList<Statement>();
		
		// iterate over subjects and predicates to find changed objects
		// iterate over added subjects
		LOG.debug(addedStatementModel.listSubjects().toList().size() + " individuals only found in updated ontology");
		LOG.debug(removedStatementModel.listSubjects().toList().size() + " individuals only found in base ontology");
		ResIterator addedSubjectsIterator = addedStatementModel.listSubjects();
		while (addedSubjectsIterator.hasNext()) {
			Resource addedSubject = addedSubjectsIterator.nextResource();
		
			// iterate over removed subjects
			ResIterator removedSubjectIterator = removedStatementModel.listSubjects();
			while (removedSubjectIterator.hasNext()) {
				Resource removedSubject = removedSubjectIterator.nextResource();
				// LOG.debug(addedSubject.getLocalName() + " <=> " + removedSubject.getLocalName());
				
				if (addedSubject.getURI().equals(removedSubject.getURI())) {
					// equal subjectURIs found
					LOG.debug("Equal subjectURIs found: " + addedSubject.getLocalName()); 
					LOG.debug("Check for equal predicateURIs...");
					
					// iterate over predicates of added subject
					StmtIterator addedStatementIterator = addedSubject.listProperties();
					while (addedStatementIterator.hasNext()) {
						Statement addedStatement = addedStatementIterator.nextStatement();
						
						// iterate over predicates of removed subject
						StmtIterator removedStatementIterator = removedSubject.listProperties();
						while (removedStatementIterator.hasNext()) {
							Statement removedStatement = removedStatementIterator.nextStatement();
							
							if (addedStatement.getPredicate().getURI().equals(removedStatement.getPredicate().getURI())) {
								// equal predicateURIs found
								LOG.debug("Equal predicateURIs found: " + addedStatement.getPredicate().getLocalName());
								
								if (!addedStatement.getObject().equals(removedStatement.getObject())) {
									Change change = new Change(this.priorVersion, this.version, removedStatement, addedStatement);
									if (change.getType().equals(ChangeType.UNTOUCHED))
										continue;
									this.addChange(change);
									addedStatements.add(addedStatement);
									removedStatements.add(removedStatement);
									LOG.debug("Added change to changeset: " + addedStatement.toString());
									if (LOG.isInfoEnabled()) changeCount++;
								}
							}
						}
					}
				}
			}
		}
		
		for (Statement statement : addedStatementModel.listStatements().toList()) {
			if (!addedStatements.contains(statement)) {
				this.addAddition(new Change(null, this.version, null, statement));
				LOG.debug("Added addition to changeset: " + statement.toString());
				if (LOG.isInfoEnabled()) { additionCount++; }
			}
		}
		for (Statement statement : removedStatementModel.listStatements().toList()) {
			if (!removedStatements.contains(statement)) {
				this.addRemoval(new Change(this.priorVersion, null, statement, null));
				LOG.debug("Added removal to changeset: " + statement.toString());
				if (LOG.isInfoEnabled()) { removalCount++; }	
			}
		}
		
		if (LOG.isInfoEnabled()) {
			String message = "Compared ontologies. " + logTime(System.nanoTime() - startTime) + " (";
			if (additionCount < 1 && removalCount < 1 && changeCount < 1)
				message += "equal ontologies";
			else {
				if (additionCount > 0) { 
					message += additionCount + " added";
					if (removalCount > 0 || changeCount > 0)
						message += ", ";
				}
				if (removalCount > 0) {
					message += removalCount + " removed";
					if (changeCount > 0)
						message += ", ";
				}
				if (changeCount > 0)
					message += changeCount + " changed";
			}
			LOG.info(message + ")");
		}
	}
	

	// getter and setter for changes by type
	
	public Set<Change> getAdditions() {
		return changeSetsByType.get(ChangeType.ADDITION);
	}
	
	public Set<Change> getRemovals() {
		return changeSetsByType.get(ChangeType.REMOVAL);
	}
	
	public Set<Change> getChanges() {
		return changeSetsByType.get(ChangeType.CHANGE);
	}
	
	private Set<Change> allChanges = null; 
	public Set<Change> getAll() {
		if (allChanges == null) {
			allChanges = new HashSet<Change>();
			allChanges.addAll(getAdditions());
			allChanges.addAll(getRemovals());
			allChanges.addAll(getChanges());
		}
		return allChanges;
	}
	
	public Set<Change> getAdditionsBySubjectURI(String subjectURI) {
		if (!changesByTypeAndSubjectURI.get(ChangeType.ADDITION).containsKey(subjectURI))
			changesByTypeAndSubjectURI.get(ChangeType.ADDITION).put(subjectURI, new HashSet<Change>());
		return changesByTypeAndSubjectURI.get(ChangeType.ADDITION).get(subjectURI);
	}
	
	public Set<Change> getRemovalsBySubjectURI(String subjectURI) {
		if (!changesByTypeAndSubjectURI.get(ChangeType.REMOVAL).containsKey(subjectURI))
			changesByTypeAndSubjectURI.get(ChangeType.REMOVAL).put(subjectURI, new HashSet<Change>());
		return changesByTypeAndSubjectURI.get(ChangeType.REMOVAL).get(subjectURI);
	}
	
	public Set<Change> getChangesBySubjectURI(String subjectURI) {
		if (!changesByTypeAndSubjectURI.get(ChangeType.CHANGE).containsKey(subjectURI))
			changesByTypeAndSubjectURI.get(ChangeType.CHANGE).put(subjectURI, new HashSet<Change>());
		return changesByTypeAndSubjectURI.get(ChangeType.CHANGE).get(subjectURI);
	}
	
	public Set<Change> getAllBySubjectURI(String subjectURI) {
		Set<Change> result = new HashSet<Change>();
		result.addAll(getAdditionsBySubjectURI(subjectURI));
		result.addAll(getRemovalsBySubjectURI(subjectURI));
		result.addAll(getChangesBySubjectURI(subjectURI));
		return result;
	}
	
	public Map<String, String> getAddedSubjectURITypesURIMap() {
		return subjectTypesBySubjectURI.get(ChangeType.ADDITION);
	}
	
	public Map<String, String> getRemovedSubjectURITypeURIMap() {
		return subjectTypesBySubjectURI.get(ChangeType.REMOVAL);
	}

	public Map<ChangeType, Set<Change>> getAllTyped() {
		return changeSetsByType;
	}

	public void addAddition(Change change) {
		this.getAll().add(change);
		this.getAdditions().add(change);
		this.getAdditionsBySubjectURI(change.getSubjectURI()).add(change);
		if (change.hasTypePredicate())
			this.getAddedSubjectURITypesURIMap().put(change.getSubjectURI(), change.getNewObjectValue());
	}
	
	public void addRemoval(Change change) {
		this.getAll().add(change);
		this.getRemovals().add(change);
		this.getRemovalsBySubjectURI(change.getSubjectURI()).add(change);
		if (change.hasTypePredicate())
			this.getRemovedSubjectURITypeURIMap().put(change.getSubjectURI(), change.getNewObjectValue());
	}
	
	public void addChange(Change change) {
		this.getAll().add(change);
		this.getChanges().add(change);
		this.getChangesBySubjectURI(change.getSubjectURI()).add(change);
	}
	
	
	// Versioned methods
	
	public Version getVersion() {
		return version;
	}
	
	public Version getPriorVersion() {
		return priorVersion;
	}
	
	@Override
	public int compareTo(ChangeSet o) {
		return version.compareTo(o.version);
	}
	
		
	// Set methods

	public boolean contains(Change change) {
		return this.getAll().contains(change);
	}
	
	public boolean containsAdditon(Change change) {
		return this.getAdditions().contains(change);
	}
	
	public boolean containsRemoval(Change change) {
		return this.getRemovals().contains(change);
	}
	
	public boolean containsChange(Change change) {
		return this.getChanges().contains(change);
	}

	public boolean hasPriorVersion() {
		return priorVersion != null;
	}

	@Override
	public String toString() {
		return "ChangeSet: " + priorVersion.toString() + " <=> " + version.toString();
	}
}
