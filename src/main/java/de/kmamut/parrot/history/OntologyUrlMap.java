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
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * Extends HashMap with capability to handle ontology URL mappings.
 * @author Knut Müller
 */
public class OntologyUrlMap extends HashMap<String, String> {
	
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(OntologyUrlMap.class);
	
	private File localDirectory = null;
	private String uriPrefix = null;
	
	/**
	 * Construct empty ontology URL map and set directory with local ontology files.
	 * @param localDirectory Directory that contains local ontology files.
	 * @param uriPrefix URL prefix that share all ontologies of map.
	 * @throws IOException if localDicrectory does not exist or can nor be read.
	 */
	public OntologyUrlMap(String localDirectory, String uriPrefix) {
		super();
		if (null != localDirectory) {
			File localDir = new File(localDirectory);
			if (!localDir.exists() || !localDir.isDirectory())
				LOG.error("Can't find diretory: " + localDir.getAbsolutePath());
			else
				this.localDirectory = localDir;			
		}
		if (null != uriPrefix) {
			URI prefix = URI.create(uriPrefix);
			if (prefix.isAbsolute() && prefix.getFragment() == null && prefix.getQuery() == null)
				this.uriPrefix = uriPrefix.endsWith("/") ? uriPrefix : (uriPrefix + "/");
			else {
				LOG.error("Invalid uri prefix: " + uriPrefix);
			}
		}
	}
	
	public OntologyUrlMap() {
		this(null, null);
	}
	
	@Override
	/**
	 * Add mapping from original ontology URL to local ontology file.
	 * The local file has to be in the local directory that was set on construction.
	 * @param ontologyURI The ontology's URI. If not a full URI, the it is append to the prefix; if prefix is set.
	 * @param localOntologyFilename The local filename of the ontology.
	 */
	public String put(String ontologyURI, String localOntologyFilename) {
		File ontologyFile = new File(localDirectory.getAbsolutePath(), localOntologyFilename);
		if (!ontologyFile.canRead())
			LOG.error("Can't read ontology file: " + ontologyFile.getAbsolutePath());
		else if (URI.create(ontologyURI).isAbsolute())
			return super.put(ontologyURI, "file:" + ontologyFile.getAbsolutePath());
		else
			return super.put(uriPrefix + ontologyURI, "file:" + ontologyFile.getAbsolutePath());
		return null;
	}
	
}
