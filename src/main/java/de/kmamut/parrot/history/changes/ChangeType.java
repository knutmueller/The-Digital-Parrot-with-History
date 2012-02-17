package de.kmamut.parrot.history.changes;

public enum ChangeType {
	ADDITION, REMOVAL, CHANGE, UNTOUCHED;
	
	public String toOntClassName() {
		switch (this) {
		case ADDITION: return "Addition";
		case REMOVAL: return "Removal";
		case CHANGE: return "Change";
		case UNTOUCHED: return "Untouched";
		default: return null;
		}
	}
	
	public String toIndividualName() {
		switch (this) {
		case ADDITION: return "addition-";
		case REMOVAL: return "removal-";
		case CHANGE: return "change-";
		case UNTOUCHED: return "untouched-";
		default: return null;
		}
	}
}

