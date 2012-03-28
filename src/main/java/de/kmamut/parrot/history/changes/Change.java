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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class Change implements Comparable<Change>{
	
	private static final Logger LOG = Logger.getLogger(Change.class);

	private ChangeType type = null;
	
	private String subjectURI = null;
	private String predicateURI = null;
	private String datatypeURI = null;
	private String language = null;
	private String oldObjectValue = null;
	private String newObjectValue = null;
	
	private Statement oldStatement = null;
	private Statement newStatement = null;
	
	private boolean hasLiteralAsObject = false;
	private boolean hasLabelAsObject = false;
	
	private Version version = null;
	private Version priorVersion = null;
		
	public Change(Version priorVersion, Version version, Statement oldStatement, Statement newStatement) {
		LOG.setLevel(Level.ALL);
		
		this.priorVersion = priorVersion;
		this.version = version;
		if (priorVersion == null && version == null)
			throw new IllegalArgumentException("At least priorVersion or Version must not be null.");
		
		this.oldStatement = oldStatement;
		this.newStatement = newStatement;
		if (oldStatement == null && newStatement == null)
			throw new IllegalArgumentException("At least old or new statement must not be null.");
		
		Statement statement = (newStatement == null) ? oldStatement : newStatement ;
		this.subjectURI = statement.getSubject().getURI();
		if (this.subjectURI == null || this.subjectURI.isEmpty())
			throw new IllegalStateException("SubjectURI must not be null or empty.");
		this.predicateURI = statement.getPredicate().getURI();
		if (this.subjectURI == null || this.subjectURI.isEmpty())
			throw new IllegalStateException("PredicateURI must not be null or empty.");
		this.oldObjectValue = extractObjectFromStatement(oldStatement);
		this.newObjectValue = extractObjectFromStatement(newStatement);
		
		if (oldStatement == null)
			type = ChangeType.ADDITION;
		else if (newStatement == null)
			type = ChangeType.REMOVAL;
		else if (oldStatement != null && newStatement != null) {
			validateObjects(oldStatement, newStatement);
			type = ChangeType.CHANGE;
		} else
			throw new IllegalStateException("Change type is not one of Addition, Removal, or Change.");
	}
	
	public Change(String subjectURI, String predicateURI, String oldObjectValue, String newObjectValue) {
		initializeChange(subjectURI, predicateURI, oldObjectValue, newObjectValue);
		this.hasLiteralAsObject = false;
	}
	
	public Change(String subjectURI, String predicateURI, String oldObjectValue, String newObjectValue, String datatypeURI, String language) {
		initializeChange(subjectURI, predicateURI, oldObjectValue, newObjectValue);
		this.hasLiteralAsObject = true;
		this.datatypeURI = datatypeURI;
		this.hasLabelAsObject = predicateURI.equals(RDFS.label.getURI());
		this.language = (language.isEmpty() ? null : language);
	}
	
	private void initializeChange(String subjectURI, String predicateURI, String oldObjectValue, String newObjectValue) {
		// check for valid arguments
		if (subjectURI == null)
			throw new IllegalArgumentException("Subject URI must not be null.", new NullPointerException());
		if (predicateURI == null)
			throw new IllegalArgumentException("Predicate URI must not be null.", new NullPointerException());
		// set change type
		if (oldObjectValue == null && newObjectValue == null) {
			type = ChangeType.UNTOUCHED;
			LOG.warn("Created an untouched change.");
		}
		else if (oldObjectValue == null)
			type = ChangeType.ADDITION;
		else if (newObjectValue == null)
			type = ChangeType.REMOVAL;
		else // oldObjectValue != null && newObjectValue != null
			type = ChangeType.CHANGE;
		// set URIs
		this.subjectURI = subjectURI;
		this.predicateURI = predicateURI;
		this.oldObjectValue = oldObjectValue;
		this.newObjectValue = newObjectValue;
		this.datatypeURI = null;
	}
	
	public boolean isLiteralChange() {
		return hasLiteralAsObject;
	}
	
	public boolean isLabelChange() {
		return hasLabelAsObject;
	}
	
	public boolean isResourceChange() {
		return !hasLiteralAsObject;
	}
	
	public boolean hasStatements() {
		switch (type) {
		case ADDITION:
			return oldStatement == null && newStatement != null;
		case REMOVAL:
			return oldStatement != null && newStatement == null;
		case CHANGE:
			return oldStatement != null && newStatement != null;
		default:
			return false;
		}
	}
	
//	public void setStatements(Statement oldStatement, Statement newStatement) {
//		switch(type) {
//		case ADDITION:
//			if (oldStatement != null || newStatement == null)
//				throw new IllegalArgumentException("For an Addition old Statement must be null and new Statement must not be null.");
//			validateSubject(newStatement);
//			validatePredicate(newStatement);
//			validateObject(newStatement);
//			this.newStatement = newStatement;
//			this.hasLiteralAsObject = newStatement.getObject().isLiteral();
//			this.hasLabelAsObject = newStatement.getObject().isLiteral() && newStatement.getPredicate().equals(RDFS.label);
//			break;
//		case REMOVAL:
//			if (oldStatement == null || newStatement != null)
//				throw new IllegalArgumentException("For an Removal old Statement must not be null and new Statement must be null.");
//			validateSubject(oldStatement);
//			validatePredicate(oldStatement);
//			validateObject(oldStatement);
//			this.oldStatement = oldStatement;
//			this.hasLiteralAsObject = oldStatement.getObject().isLiteral();
//			this.hasLabelAsObject = oldStatement.getObject().isLiteral() && oldStatement.getPredicate().equals(RDFS.label);
//			break;
//		case CHANGE:
//			if (oldStatement == null || newStatement == null)
//				throw new IllegalArgumentException("For an Change old Statement and new Statement must not be null.");
//			validateSubject(newStatement);
//			validateSubject(oldStatement);
//			validatePredicate(newStatement);
//			validatePredicate(oldStatement);
//			validateObjects(oldStatement, newStatement);
//			this.oldStatement = oldStatement;
//			this.newStatement = newStatement;
//			this.hasLiteralAsObject = oldStatement.getObject().isLiteral();
//			this.hasLabelAsObject = oldStatement.getObject().isLiteral() && oldStatement.getPredicate().equals(RDFS.label);
//			break;
//		default:
//			throw new IllegalStateException("This change's type should be one of ADDITION, REMOVAL, or CHANGE.");
//		}
//	}
	
	private void validateSubject(Statement statement) {
		if (statement == null)
			throw new IllegalArgumentException("Statement must not be null.");
		else if (!this.subjectURI.equals(statement.getSubject().getURI()))
			throw new IllegalArgumentException("Incompatible subject: " + subjectURI + " (Change) != " + statement.getSubject().getURI() + " (Statement).");
	}
	
	private void validatePredicate(Statement statement) {
		if (statement == null)
			throw new IllegalArgumentException("Statement must not be null.");
		else if (!this.predicateURI.equals(statement.getPredicate().getURI()))
			throw new IllegalArgumentException("Incompatible predicate: " + predicateURI + " (Change) != " + statement.getPredicate().getURI() + " (Statement).");
	}
	
	private void validateObject(Statement statement) {
		if (statement == null)
			throw new IllegalArgumentException("Statement must not be null.");
		else if (!statement.getObject().isLiteral() && !statement.getObject().isURIResource())
			throw new IllegalArgumentException("Incompatible Object: Object is neigther Literal nor URIResource.");
	}
	
	private void validateObjects(Statement statementA, Statement statementB) {
		validateObject(statementA);
		validateObject(statementB);
		if (statementA.getObject().isLiteral() != statementB.getObject().isLiteral() ||
				statementA.getObject().isURIResource() != statementB.getObject().isURIResource())
			throw new IllegalArgumentException("Incompatible Objects: Both Objects must be same, Literal or URIResoource.");
	}
	
	private String extractObjectFromStatement(Statement statement) {
		if (statement == null)
			return null;
		else {
			RDFNode object = statement.getObject();
			if (object.isResource()) 
				return object.asResource().getURI();
			else { // statement.getObject().isLiteral()
				this.hasLiteralAsObject = true;
				this.datatypeURI = object.asLiteral().getDatatypeURI();
				this.language = object.asLiteral().getLanguage();
				if (this.predicateURI.equals(RDFS.label.getURI()))
					this.hasLabelAsObject = true;
				return statement.getObject().asLiteral().getLexicalForm();
			}
		}
	}
	
	public ChangeType getType() {
		return type;
	}
	
	public boolean isAddition() {
		return type == ChangeType.ADDITION;
	}
	
	public boolean isRemoval() {
		return type == ChangeType.REMOVAL;
	}
	
	public boolean isChange() {
		return type == ChangeType.CHANGE;
	}
	
	public boolean hasTypePredicate() {
		return RDF.type.getURI().equals(predicateURI);
	}

	public String getSubjectURI() {
		return subjectURI;
	}
	
	public String getPredicateURI() {
		return predicateURI;
	}
	
	public String getOldObjectValue() {
		return oldObjectValue;
	}
	
	public String getNewObjectValue() {
		return newObjectValue;
	}

	public String getDatatypeURI() {
		return datatypeURI;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public Statement getOldStatement() {
		return oldStatement;
	}
	
	public Statement getNewStatement() {
		return newStatement;
	}
	
	
	// versioned object behaviour
	
	/**
	 * Get the Version of the ontology which this Change results in.  
	 * @return The Version of the updated (new) ontology.
	 */
	public Version getVersion() {
		return version;
	}
	
	/**
	 * Get the Version of the ontology which this Change changes.  
	 * @return The Version of the updated (new) ontology.
	 */
	public Version getPriorVersion() {
		return priorVersion;
	}
	
	@Override
	public int compareTo(Change o) {
		return version.compareTo(o.version);
	}
	
	
	@Override
	public String toString() {
		String output = type.toString().toLowerCase() + ": ";
		if (oldStatement != null)
			output += oldStatement.toString();
		else if (oldObjectValue != null) {
			output += "[" + subjectURI + ", " + predicateURI + ", " + oldObjectValue;
			if (datatypeURI != null) {
				if (predicateURI.equals(RDFS.label.getURI()))
					output += "@";
				else
					output += "^^";
				output += datatypeURI;
			}
			output += "]";
		} else
			output += "null";
		output += " <=> ";
		if (newStatement != null)
			output += newStatement.toString();
		else if (newObjectValue != null) {
			output += "[" + subjectURI + ", " + predicateURI + ", " + newObjectValue;
			if (datatypeURI != null && !datatypeURI.isEmpty())
				output += "^^" + datatypeURI;
			if (language != null && !language.isEmpty())
				output += "@" + language;
			output += "]";
		} else
			output += "null";
		return output;
	}

	public void setNewStatement(Statement newStatement) {
		validateSubject(newStatement);
		validatePredicate(newStatement);
		validateObject(newStatement);
		this.newStatement = newStatement;
		this.hasLiteralAsObject = newStatement.getObject().isLiteral();
		this.hasLabelAsObject = newStatement.getPredicate().equals(RDFS.label);
	}
	
	public void setOldStatement(Statement oldStatement) {
		validateSubject(oldStatement);
		validatePredicate(oldStatement);
		validateObject(oldStatement);
		this.newStatement = oldStatement;
		this.hasLiteralAsObject = oldStatement.getObject().isLiteral();
		this.hasLabelAsObject = oldStatement.getPredicate().equals(RDFS.label);
	}
	
	
}
