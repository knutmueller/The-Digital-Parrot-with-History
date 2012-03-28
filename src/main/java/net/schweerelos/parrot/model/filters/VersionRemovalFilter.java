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

import java.util.HashSet;
import java.util.Set;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.VersionedNodeWrapper;
import de.kmamut.parrot.history.changes.ChangeType;
import de.kmamut.parrot.history.changes.Version;

public class VersionRemovalFilter extends VersionFilter {

	private Set<NodeWrapper> applicableNodeWrappers;
	
	public VersionRemovalFilter(Version version) {
		super(version, Mode.VERSION);
	}
	
	@Override
	public ChangeType getType() {
		return ChangeType.REMOVAL;
	}

	@Override
	protected Set<NodeWrapper> extractApplicableNodeWrappers(ParrotModel parrotModel) {
		if (applicableNodeWrappers == null) {
			applicableNodeWrappers = new HashSet<NodeWrapper>();
			for (NodeWrapper nodeWrapper : parrotModel.getAllNodeWrappers()) {
				VersionedNodeWrapper wrapper = (VersionedNodeWrapper) nodeWrapper;
				wrapper.setActiveVersion(super.getVersion());
				if (wrapper.isRemoved())
					applicableNodeWrappers.add(nodeWrapper);
			}
		}
		return applicableNodeWrappers;
	}

}
