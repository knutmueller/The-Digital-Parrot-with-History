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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import de.kmamut.parrot.history.changes.Version;

public class HistoryListNavigator extends AbstractHistoryNavigator implements NavigatorComponent {

	private static final long serialVersionUID = 1L;

	private static final String NAME = "History List";
	private static final String ACCELERATOR_KEY = "L";
	
	private TreeMap<Integer, JButton> historyButtons = new TreeMap<Integer, JButton>();

	@Override
	public String getNavigatorName() {
		return NAME;
	}

	@Override
	public String getAcceleratorKey() {
		return ACCELERATOR_KEY;
	}

	public HistoryListNavigator(HistoryNavigatorHelper helper) {
		super(NAME, helper);
		setLayout(new BorderLayout());
	}

	@Override
	protected void initialise() {
		JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
		
		JButton todayButton = new JButton("today");
		todayButton.setEnabled(false);
		buttonPanel.add(todayButton);
		
		DateTimeFormatter labelFormatter = new DateTimeFormatterBuilder()
			.appendLiteral("<html><center>")
			.appendDayOfWeekText()
			.appendLiteral("<br/>")
			.appendDayOfMonth(1)
			.appendLiteral(' ')
			.appendMonthOfYearText()
			.appendLiteral(' ')
			.appendYear(4, 4)
			.appendLiteral("</center></html>")
			.toFormatter();
	
		List<Version> versions = getVersions();
		for (int i = versions.size() - 1 ; i >= 0; i--) {
			final int state = i;
			String label = labelFormatter.print(versions.get(i).getDateTime());
			JButton historyButton = new JButton(new AbstractAction(label) {
				private static final long serialVersionUID = 1L;
				@Override
				public void actionPerformed(ActionEvent e) {
					JButton source = (JButton) e.getSource();
					if (!source.isSelected()) {
						setState(state);
					}
				}
			});
			historyButtons.put(state, historyButton);
			buttonPanel.add(historyButton);
		}
		historyButtons.lastEntry().getValue().setSelected(true);
		
		JScrollPane scrollPane = new JScrollPane(buttonPanel);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.NORTH);
		
		super.addHistoryStateListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent e) {
				historyButtons.get(e.getOldValue()).setSelected(false);
				historyButtons.get(e.getNewValue()).setSelected(true);
			}
		});
	}

	@Override
	protected void resetLabelsColor(Color color) {
		for (JButton historyButton : historyButtons.values())
			historyButton.setForeground(color);
	}
	
	@Override
	protected void setLabelColor(Integer state, Color color) {
		if (color != null && state != null)
			historyButtons.get(state).setForeground(color);
	}

}
