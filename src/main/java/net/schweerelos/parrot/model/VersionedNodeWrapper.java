package net.schweerelos.parrot.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Literal;

import de.kmamut.parrot.history.HistoryModel;
import de.kmamut.parrot.history.changes.Version;

public class VersionedNodeWrapper extends NodeWrapper {

	private TreeSet<Version> availableVersions = null;
	private TreeSet<Version> validVersions = new TreeSet<Version>();
	private Version activeVersion = null;
	
	private List<Version> availableVersionsList = new ArrayList<Version>();
	
	private Version addedInVersion = null;
	private Version removedInVersion = null;
	private TreeSet<Version> changedInVersion = new TreeSet<Version>(); 
	private TreeMap<Version, String> versionedLabels = new TreeMap<Version, String>();
	
	public VersionedNodeWrapper(HistoryModel historyModel, Version version, OntResource node) {
		super(node);
		versionedLabels.put(version, extractLabel(node));
		initialise(historyModel, version);
	}
	
	public VersionedNodeWrapper(HistoryModel historyModel, Version version, Literal literal) {
		super(literal);
		this.versionedLabels.put(version, extractLabel(literal));
		initialise(historyModel, version);
	}
	
	private void initialise(HistoryModel historyModel, Version version) {
		this.availableVersionsList = historyModel.getVersions();
		this.availableVersions = new TreeSet<Version>(availableVersionsList);
		addVersion(version);
	}

	public VersionedNodeWrapper addVersion(Version version) {
		this.validVersions.add(version);
		this.addedInVersion = validVersions.first();
		this.removedInVersion = availableVersions.higher(validVersions.last());
		if (availableVersions.lower(version) != null) {
			String oldLabel = versionedLabels.get(availableVersions.lower(version));
			String newLabel = versionedLabels.get(version);
			if (oldLabel != null & newLabel != null && !oldLabel.equals(newLabel))
				changedInVersion.add(version);
		}
		return this;
	}
	
	public VersionedNodeWrapper addResourceVersion(Version version, OntResource node) {
		this.versionedLabels.put(version, extractLabel(node));
		return addVersion(version);
	}
	
	public VersionedNodeWrapper addLiteralVersion(Version version, Literal literal) {
		this.versionedLabels.put(version, extractLabel(literal));
		return addVersion(version);
	}
	
	public boolean isAdded() {
		return activeVersion != null && addedInVersion.equals(activeVersion);
	}
	
	public boolean isRemoved() {
		return activeVersion != null && removedInVersion != null && removedInVersion.equals(activeVersion);
	}
	
	public boolean isChanged() {
		return activeVersion != null && changedInVersion.contains(activeVersion); 
	}
	
	public boolean isHidden() {
		return !isRemoved() && (activeVersion != null && !validVersions.contains(activeVersion));
	}
	
	@Override
	public boolean isHereTooSelected() {
		return !isHidden() && super.isHereTooSelected();
	}
	
	@Override
	public boolean isHighlighted() {
		return !isHidden() && super.isHighlighted();
	}
	
	public void setActiveVersion(Version version) {
		activeVersion = version;
	}
	
	
	// from NodeWrapper
	
	@Override
	public String toString() {
		if ((activeVersion != null) && (versionedLabels.containsKey(activeVersion)))
			return versionedLabels.get(activeVersion);
		return versionedLabels.lastEntry().getValue();
	}

	public Integer getAddionenState() {
		return availableVersionsList.indexOf(addedInVersion);
	}
	
	public Integer getRemovalState() {
		if (removedInVersion == null)
			return null;
		return availableVersionsList.indexOf(removedInVersion);
	}

	public Collection<Integer> getChangeStates() {
		List<Integer> result = new ArrayList<Integer>();
		for (Version version : changedInVersion)
			result.add(availableVersionsList.indexOf(version));
		return result;
	}
	
	
}
