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
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class Version implements Comparable<Version> {

	private List<Integer> subVersions = new ArrayList<Integer>(); 
	private String versionString;
	private String ontologyIRI;
	private String versionIRI;
	private String priorVersionIRI;
	private DateTime datetime;
	private String versionInfo;
	
	/**
	 * Construct version from a string. The ontology and version IRIs link to the actual ontologys.  
	 * @param versionInfo A string which consists of a version number and an optional date time.
	 * The version number must have an optional "v" followed by integers separated by single dots, like v1, 2.3, or v2.3.435).
	 * The optional date and time must be in ISO 8601 format and is separated from the version number by a single space.
	 * @param ontologyIRI IRI to ontology of this version. (Usually, this links to the current version of the ontolgoy.)
	 * @param versionIRI IRI to the ontology of this version.
	 * @param priorVersionIRI IRI to the ontogy of the previous version.
	 */
	public Version(String versionInfo, String ontologyIRI, String versionIRI, String priorVersionIRI) {
		String pattern = "v?(\\d+(?:\\.\\d+)*)(?: (.*))?$";
		if (!versionInfo.matches(pattern))
			throw new IllegalArgumentException("Version info string is not in a valid format.");
		
		this.versionInfo = versionInfo;
		this.versionString = versionInfo.replaceFirst(pattern, "$1");
		for (String subVersionString : versionString.split("\\."))
			subVersions.add(new Integer(subVersionString));
		
		String datetimeString = versionInfo.replaceFirst(pattern, "$2");
		if (datetimeString == null || datetimeString.isEmpty())
			this.datetime = null;
		else {
			DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
			this.datetime = parser.parseDateTime(datetimeString);
		}
		
		this.ontologyIRI = ontologyIRI;
		this.versionIRI = versionIRI;
		this.priorVersionIRI = priorVersionIRI;
	}
	
	@Override
	public int compareTo(Version other) {
		int index = -1;
		while (true) {
			index++;
			Integer thisSubVersion = null, otherSubVersion = null;
			boolean thisIsOutOfBounds = false, otherIsOutOfBounds = false;
			try {
				thisSubVersion = this.subVersions.get(index);  
			} catch(IndexOutOfBoundsException e) {
				thisSubVersion = new Integer(0);
				thisIsOutOfBounds = true;
			}
			try {
				otherSubVersion = other.subVersions.get(index);  
			} catch(IndexOutOfBoundsException e) {
				otherSubVersion = new Integer(0);
				otherIsOutOfBounds = true;
			}
			if (thisIsOutOfBounds && otherIsOutOfBounds)
				return 0;
			if (!(thisSubVersion == otherSubVersion))
				return thisSubVersion.compareTo(otherSubVersion);
		}
	}
	
	@Override
	public String toString() {
		return versionString;
	}

	public boolean hasOntologyIRI() {
		return ontologyIRI != null && !ontologyIRI.isEmpty();
	}

	public boolean hasVersionIRI() {
		return versionIRI != null && !versionIRI.isEmpty();
	}
	
	public boolean hasPriorVersionIRI() {
		return priorVersionIRI != null && !priorVersionIRI.isEmpty();
	}
	
	public String getOntologyIRI() {
		return ontologyIRI;
	}
	
	public String getVersionIRI() {
		return versionIRI;
	}
	
	public String getPriorVeresionIRI() {
		return priorVersionIRI;
	}
	
	public String getVersionInfo() {
		return versionInfo;
	}
	
	public DateTime getDateTime() {
		return datetime;
	}
	
}

