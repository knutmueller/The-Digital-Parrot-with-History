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


package net.schweerelos.parrot.model.filters;

import de.kmamut.parrot.history.changes.ChangeType;
import de.kmamut.parrot.history.changes.Version;

import net.schweerelos.parrot.model.NodeWrapper;

public abstract class VersionFilter extends SimpleFilter {
	
	private Version version = null;
	
	public VersionFilter(Version version, Mode mode) {
		this.version = version;
		setMode(mode);
	}

	public abstract ChangeType getType();
	
	public Version getVersion() {
		return version;
	}
	
	@Override
	protected boolean matches(NodeWrapper nodeWrapper) {
		return true;
	}

}
