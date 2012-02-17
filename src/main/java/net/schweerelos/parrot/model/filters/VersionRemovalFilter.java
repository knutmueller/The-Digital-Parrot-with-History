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
