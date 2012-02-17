package net.schweerelos.parrot.model.filters;

import java.util.HashSet;
import java.util.Set;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.VersionedNodeWrapper;
import de.kmamut.parrot.history.changes.ChangeType;
import de.kmamut.parrot.history.changes.Version;

public class VersionRestrictingFilter extends VersionFilter {

	private Set<NodeWrapper> applicableNodeWrappers;
	
	public VersionRestrictingFilter(Version version) {
		super(version, Mode.RESTRICT);
	}
	
	@Override
	public ChangeType getType() {
		return ChangeType.UNTOUCHED;
	}

	@Override
	protected Set<NodeWrapper> extractApplicableNodeWrappers(ParrotModel parrotModel) {
		if (applicableNodeWrappers == null) {
			applicableNodeWrappers = new HashSet<NodeWrapper>();
			for (NodeWrapper nodeWrapper : parrotModel.getAllNodeWrappers()) {
				VersionedNodeWrapper wrapper = (VersionedNodeWrapper) nodeWrapper;
				wrapper.setActiveVersion(super.getVersion());
				if (wrapper.isHidden())
					applicableNodeWrappers.add(nodeWrapper);
			}
		}
		return applicableNodeWrappers;
	}

}
