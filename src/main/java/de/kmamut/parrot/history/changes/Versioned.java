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
