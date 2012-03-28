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


package net.schweerelos.parrot.ui;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.kmamut.parrot.history.HistoryModel;
import de.kmamut.parrot.history.changes.Version;
import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.ParrotModelWithHistory;
import net.schweerelos.parrot.model.filters.VersionAdditionFilter;
import net.schweerelos.parrot.model.filters.VersionChangeFilter;
import net.schweerelos.parrot.model.filters.VersionRemovalFilter;
import net.schweerelos.parrot.model.filters.VersionRestrictingFilter;

public class HistoryNavigatorHelper {

	protected HistoryModel historyModel;
	
	private Filter lastAdditionFilter;
	private Filter lastRemovalFilter;
	private Filter lastChangeFilter;
	private Filter lastRestrictingFilter;
	private Integer lastState;
	
	private ArrayList<Filter> additionFilters = new ArrayList<Filter>();
	private ArrayList<Filter> removalFilters = new ArrayList<Filter>();
	private ArrayList<Filter> changeFilters = new ArrayList<Filter>();
	private ArrayList<Filter> restrictingFilters = new ArrayList<Filter>();

	private Map<String, Boolean> visibleHistoryNavigators = new HashMap<String, Boolean>();
	
	private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
	private final String STATE_PROPERTY = "state";
	
	public void setModel(ParrotModel model) {
		if (model == null)
			throw new IllegalArgumentException("ParrotModel must not be null.");
		if (!(model instanceof ParrotModelWithHistory))
			throw new IllegalArgumentException("ParrotModel must be instance of ParrotModelWithHistory.");
		this.historyModel = ((ParrotModelWithHistory) model).getHistoryModel();
		int i = 0;
		for (Version version : historyModel.getVersions()) {
			additionFilters.add(new VersionAdditionFilter(version));
			removalFilters.add(new VersionRemovalFilter(version));
			changeFilters.add(new VersionChangeFilter(version));
			restrictingFilters.add(new VersionRestrictingFilter(version));
			i++;
		}
		lastAdditionFilter = additionFilters.get(i-1);
		lastRemovalFilter = removalFilters.get(i-1);
		lastChangeFilter = changeFilters.get(i-1);
		lastRestrictingFilter = restrictingFilters.get(i-1);
		setState(i-1);
	}

	public Filter getLastAdditionFilter() {
		return lastAdditionFilter;
	}

	public Filter getLastRemovalFilter() {
		return lastRemovalFilter;
	}

	public Filter getLastChangeFilter() {
		return lastChangeFilter;
	}
	
	public Filter getLastRestrictingFilter() {
		return lastRestrictingFilter;
	}

	public Map<Filter, Filter> setState(Integer state) {
		Map<Filter, Filter> replacementMap = new HashMap<Filter, Filter>();
		// addition filter
		Filter newAdditionFilter = additionFilters.get(state);
		replacementMap.put(lastAdditionFilter, newAdditionFilter);
		lastAdditionFilter = newAdditionFilter;
		// removal filter
		Filter newRemovalFilter = removalFilters.get(state);
		replacementMap.put(lastRemovalFilter, newRemovalFilter);
		lastRemovalFilter = newRemovalFilter;
		// change filter
		Filter newChangeFilter = changeFilters.get(state);
		replacementMap.put(lastChangeFilter, newChangeFilter);
		lastChangeFilter = newChangeFilter;
		// restricting filter
		Filter newRestrictingFilter = restrictingFilters.get(state);
		replacementMap.put(lastRestrictingFilter, newRestrictingFilter);
		lastRestrictingFilter = newRestrictingFilter;
		// fire state changed
		Integer newState = state;
		changeSupport.firePropertyChange(STATE_PROPERTY, lastState, state);
		lastState = newState;
		return replacementMap;
	}

	public void addStateListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(STATE_PROPERTY, listener);
	}

	public List<Version> getVersions() {
		return historyModel.getVersions();
	}

	public void setActive(String name, boolean visible) {
		visibleHistoryNavigators.put(name, visible);
	}

	public boolean isActive() {
		for (boolean visible : visibleHistoryNavigators.values())
			if (visible) return true;
		return false;
	}

}
