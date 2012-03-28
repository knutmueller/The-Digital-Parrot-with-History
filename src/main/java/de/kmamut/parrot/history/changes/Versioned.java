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

public interface Versioned<T> extends Comparable<T> {
	
	/**
	 * Get the Version of this versioned Object.  
	 * @return The Version of this versioned Object.
	 */
	public Version getVersion();
	
	/**
	 * Get the Version of the predecessor of this versioned Object.  
	 * @return The Version of the predecessor of this versioned Object.
	 */
	public Version getPriorVersion();
	
}
