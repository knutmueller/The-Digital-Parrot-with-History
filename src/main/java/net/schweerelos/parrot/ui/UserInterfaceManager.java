/*
 * Copyright (C) 2012 Knut Müller,
 * Copyright (C) 2011 Andrea Schweer
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

import java.util.Properties;

import net.schweerelos.parrot.model.ParrotModelFactory.Style;


public class UserInterfaceManager {

	private Properties properties;

	public UserInterfaceManager(Properties properties) {
		this.properties = properties;
	}

	public MainViewComponent createGraphViewComponent() {
		return new GraphViewComponent();
	}

	public MainViewComponent createTableViewComponent() {
		return new TableViewComponent();
	}

	public NavigatorComponent createTimelineNavigationComponent() {
		return new TimelineNavigator();
	}

	public NavigatorComponent createMapNavigationComponent() {
		return new WebRendererMapNavigator(properties);
	}

	public NavigatorComponent createChainNavigationComponent() {
		return new ChainNavigator();
	}

	public NavigatorComponent createSearchComponent() {
		return new TextFilterComponent();
	}

	public MainViewComponent createMainViewComponent(Style type) throws UnknownStyleException {
		if (type == Style.GRAPH) {
			return createGraphViewComponent();
		} else if (type == Style.TABLE) {
			return createTableViewComponent();
		} else {
			throw new UnknownStyleException(type + " is not a recognised main view style");
		}
	}

	/**
	 * Create and return slider as history navigator component.
	 * @param helper The HistoryNavigatorHelper which provides shared data used by all history navigators. 
	 * @return HistorySliderNavigator component.
	 */
	public NavigatorComponent createHistorySliderNavigationComponent(HistoryNavigatorHelper helper) {
		return new HistorySliderNavigator(helper);
	}

	/**
	 * Create and return list as history navigator component.
	 * @param helper The HistoryNavigatorHelper which provides shared data used by all history navigators. 
	 * @return HistoryListNavigator component.
	 */
	public NavigatorComponent createHistoryListNavigationComponent(HistoryNavigatorHelper helper) {
		return new HistoryListNavigator(helper);
	}

}
