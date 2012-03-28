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

import java.util.TreeMap;

import com.hp.hpl.jena.ontology.OntModel;

import de.kmamut.parrot.history.HistoryModel;
import de.kmamut.parrot.history.changes.Version;

public interface ParrotModelWithHistory extends ParrotModel {
	
	@Override
	@Deprecated
	/**
	 * Do not use this method. Use loadData(String datafile, String historyfile) instead.
	 */
	public void loadData(String datafile);
	
	/**
	 * Initializes the data model from datafile and the histroy model from historyfile. 
	 * @param datafile File containing RDF memory data in a format readable by Jena.
	 * @param historyfile File containing RDF history data in a format readable by Jena.
	 */
	public void loadData(String datafile, String historyfile);

	/**
	 * Get the HistoryModel that holds all history information.
	 * @return HistoryModel Object.
	 */
	public HistoryModel getHistoryModel();

	/**
	 * Get OntModel for each version.
	 * @return Map from version to corresponding OntModel.
	 */
	public TreeMap<Version, OntModel> getVersionedOntModels();

}
