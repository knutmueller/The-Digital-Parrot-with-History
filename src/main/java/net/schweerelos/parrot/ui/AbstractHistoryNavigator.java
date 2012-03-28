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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.apache.log4j.Logger;

import de.kmamut.parrot.history.changes.Version;

import net.schweerelos.parrot.model.Filter;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.VersionedNodeWrapper;

public abstract class AbstractHistoryNavigator extends AbstractNavigatorPanel implements NavigatorComponent {

	private static final long serialVersionUID = 1L;

	private HistoryNavigatorHelper helper;
	private String name;
	
	private Set<NodeWrapper> highlighted = new HashSet<NodeWrapper>();
	
	public AbstractHistoryNavigator(String name, HistoryNavigatorHelper helper) {
		this.helper = helper;
		this.name = name;
	}

	@Override
	public void setModel(ParrotModel model) {
		super.setModel(model);
		helper.setModel(model);
		initialise();
	}

	/**
	 * Override this to initialise and create the view after the parrot model is set. 
	 */
	protected abstract void initialise();
	
	@Override
	public List<Action> getActionsForNode(final NodeWrapper node) {
		ArrayList<Action> result = new ArrayList<Action>();
		if (highlighted.contains(node))
			result.add(getUnHighlightAction((VersionedNodeWrapper) node));
		else
			result.add(getHighlightAction((VersionedNodeWrapper) node));
		return result;
	}

	@Override
	public List<Action> getActionsForType(NodeWrapper type) {
		return Collections.<Action>emptyList();
	}

	@Override
	protected void deactivateFilters() {
		if (getModel() == null) {
			return;
		}
		helper.setActive(name, false);
		if (helper.isActive())
			return;
		Filter lastAdditionFilter = helper.getLastAdditionFilter();
		if (lastAdditionFilter  != null) {
			removeFilter(lastAdditionFilter);
		}
		Filter lastRemovalFilter = helper.getLastRemovalFilter();
		if (lastRemovalFilter != null) {
			removeFilter(lastRemovalFilter);
		}
		Filter lastChangeFilter = helper.getLastChangeFilter();
		if (lastChangeFilter != null) {
			removeFilter(lastChangeFilter);
		}
		Filter lastRestrictingFilter = helper.getLastRestrictingFilter();
		if (lastRestrictingFilter != null) {
			removeFilter(lastRestrictingFilter);
		}
	}

	@Override
	protected void activateFilters() {
		if (getModel() == null) {
			return;
		}
		helper.setActive(name, true);
		Filter lastAdditionFilter = helper.getLastAdditionFilter();
		if (lastAdditionFilter != null) {
			applyFilter(lastAdditionFilter);
		}
		Filter lastRemovalFilter = helper.getLastRemovalFilter();
		if (lastRemovalFilter != null) {
			applyFilter(lastRemovalFilter);
		}
		Filter lastChangeFilter = helper.getLastChangeFilter();
		if (lastChangeFilter != null) {
			applyFilter(lastChangeFilter);
		}
		Filter lastRestrictingFilter = helper.getLastRestrictingFilter();
		if (lastRestrictingFilter != null) {
			applyFilter(lastChangeFilter);
		}
	}

	/**
	 * Set the history state.
	 * @param state Integer that represents the history state.
	 */
	protected void setState(int state) {
		for (Entry<Filter, Filter> entry : helper.setState(state).entrySet()) {
			replaceFilter(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * Add listener to history navigator helper to handle state changes from another history navigator.
	 * @param listener
	 */
	protected void addHistoryStateListener(PropertyChangeListener listener) {
		helper.addStateListener(listener);
	}
	
	/**
	 * Get available versions within history model
	 * @return List of Versions.
	 */
	protected List<Version> getVersions() {
		return helper.getVersions();
	}

	protected abstract void resetLabelsColor(Color color);
		
	protected abstract void setLabelColor(Integer state, Color color);
	
	private Action getHighlightAction(final VersionedNodeWrapper node) {
		return new AbstractAction("Show changes in " + name) {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent ae) {
					highlighted.add(node);
					if (!isVisible()) {
						activateFilters();
						setVisible(true);
						if (node.getRemovalState() != null)
							setState(node.getRemovalState());
					}
					Logger.getLogger(AbstractHistoryNavigator.class).warn("Highlight changes of \"" + node.toString() +  "\".");
					resetLabelsColor(Color.BLACK);
					setLabelColor(node.getAddionenState(), UIConstants.ADDITION.darker());
					setLabelColor(node.getRemovalState(), UIConstants.REMOVAL.darker());
					for (Integer state : node.getChangeStates())
						setLabelColor(state, UIConstants.CHANGE.darker().darker());
					repaint();
				}
		};
	}

	private Action getUnHighlightAction(final VersionedNodeWrapper node) {
		return new AbstractAction("Hide changes in " + name) {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent ae) {
					highlighted.remove(node);
					Logger.getLogger(AbstractHistoryNavigator.class).warn("Unhighlight changes of \"" + node.toString() +  "\".");
					resetLabelsColor(Color.BLACK);
					repaint();
				}
		};
	}



}
