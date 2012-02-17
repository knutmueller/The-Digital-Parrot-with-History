package net.schweerelos.parrot.model.filters;

import java.util.HashSet;
import java.util.Set;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.VersionedNodeWrapper;
import de.kmamut.parrot.history.changes.ChangeType;
import de.kmamut.parrot.history.changes.Version;

public class VersionAdditionFilter extends VersionFilter {

	private Set<NodeWrapper> applicableNodeWrappers;
	
	public VersionAdditionFilter(Version version) {
		super(version, Mode.VERSION);
	}
	
	public ChangeType getType() {
		return ChangeType.ADDITION;
	}

	@Override
	protected Set<NodeWrapper> extractApplicableNodeWrappers(ParrotModel parrotModel) {
		if (applicableNodeWrappers == null) {
			applicableNodeWrappers = new HashSet<NodeWrapper>();
			for (NodeWrapper nodeWrapper : parrotModel.getAllNodeWrappers()) {
				VersionedNodeWrapper wrapper = (VersionedNodeWrapper) nodeWrapper;
				wrapper.setActiveVersion(super.getVersion());
				if (wrapper.isAdded())
					applicableNodeWrappers.add(nodeWrapper);
			}
		}
		return applicableNodeWrappers;
	}

}
