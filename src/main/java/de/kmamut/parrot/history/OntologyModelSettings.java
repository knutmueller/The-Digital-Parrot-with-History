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
import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;

/**
 * Aggregates several settings which the digital parrot's ontologies have in common.  
 * @author Knut Müller
 */
public class OntologyModelSettings {
	
	protected static final String localDirectory = System.getProperty("user.home") + File.separator + ".digital-parrot" + File.separator + "owl"; 
	protected static final String uriPrefix = "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/";  
	
	private static final Map<String, String> URL_MAP = createParrotUrlMap();
	protected static OntologyUrlMap createParrotUrlMap() {
		OntologyUrlMap map = new OntologyUrlMap(localDirectory, uriPrefix);
		map.put("DigitalParrot/2009/02/DigitalParrot.owl", "DigitalParrot.owl");
		map.put("History/2011/11/History.owl", "History.owl");
		map.put("Interaction/2008/11/Interaction.owl", "Interaction.owl");
		map.put("Memories/2008/11/Memories.owl", "Memories.owl");
		map.put("Music/2011/07/Music.owl", "Music.owl");
		map.put("TimeAndPlace/2008/11/TimeAndPlace.owl", "TimeAndPlace.owl");
		return map;
	}
	
	/**
	 * Create Digital Parrot model that use URL mapping to local ontology files. 
	 * @return OntModel (OWL_MEM) with alternative URL entries.
	 */
	public static OntModel createModel() {
		return (OntModel)createModelSpec(OntModelSpec.OWL_MEM, URL_MAP).doCreateModel();
	}
	
	protected static OntModelSpec createModelSpec(Map<String, String> urlMap) {
		return createModelSpec(OntModelSpec.OWL_MEM, urlMap);
	}
	
	protected static OntModelSpec createModelSpec(OntModelSpec ontModelSpec, Map<String, String> urlMap) {
		OntModelSpec oms = new OntModelSpec(ontModelSpec);
		for (String ontologyURL : urlMap.keySet()) {
			oms.getDocumentManager().addAltEntry(ontologyURL, urlMap.get(ontologyURL));
		}
		return oms;
	}
	
	/**
	 * Mapping of short ontology prefixes to full ontology URLs.
	 */
	public static final Map<String, String> PREFIX_MAP = createPrefixMap();
    protected static Map<String, String> createPrefixMap() {
    	Map<String,String> map = new HashMap<String,String>();
    	map.put("foaf", "http://xmlns.com/foaf/0.1/");
    	map.put("interact", "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/Interaction/2008/11/Interaction.owl#");
    	map.put("parrot", "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/DigitalParrot/2009/02/DigitalParrot.owl#");
    	map.put("timeplace", "http://parrot.resnet.scms.waikato.ac.nz/Parrot/Terms/TimeAndPlace/2008/11/TimeAndPlace.owl#");
		return map;
	}
    
    
    
}
