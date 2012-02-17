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
