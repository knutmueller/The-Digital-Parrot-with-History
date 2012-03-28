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


package de.kmamut.parrot;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import javax.swing.SwingUtilities;

import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.ParrotModelFactory;
import net.schweerelos.parrot.model.ParrotModelFactory.Style;
import net.schweerelos.parrot.model.ParrotModelWithHistory;
import net.schweerelos.parrot.ui.MainViewComponent;
import net.schweerelos.parrot.ui.NavigatorComponent;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


class ParrotAppSettings {
	@Option(name = "--properties", aliases = {"-h"}, usage = "Properties file to use. Default: ~/.digital-parrot/parrot.properties" )
	public String propertiesPath = System.getProperty("user.home") + File.separator + ".digital-parrot" + File.separator + "parrot.properties";
	@Argument(index = 0, required = true, metaVar = "data", usage = "Data file with memory facts.")
	public String dataPath;
	@Argument(index = 1, required = true, metaVar = "history", usage = "History file with replaced and removed memory facts.")
	public String historyPath;  
}

public class ParrotApp {
	
	private static final Logger LOG = Logger.getLogger(ParrotApp.class);

	private ParrotAppFrame frame;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		ParrotAppSettings settings = new ParrotAppSettings();
		CmdLineParser parser = new CmdLineParser(settings);
        try {
        	parser.parseArgument(args);
            File dataFile = new File(settings.dataPath);
            final String dataPath = dataFile.getAbsolutePath(); 
            if (!dataFile.exists())
            	throw new FileNotFoundException(dataPath);
            else if (!dataFile.canRead())
            	throw new IOException(dataPath);
            File historyFile = new File(settings.historyPath);
            final String historyPath = historyFile.getAbsolutePath(); 
            if (!historyFile.exists())
            	throw new FileNotFoundException(historyPath);
            else if (!historyFile.canRead())
            	throw new IOException(historyPath);
            File propertiesFile = new File(settings.propertiesPath);
            final Properties properties = new Properties();;
			if (!propertiesFile.exists())
				throw new FileNotFoundException(propertiesFile.getAbsolutePath());
			else if (!propertiesFile.canRead())
				throw new IOException("Cannot read " + propertiesFile.getAbsolutePath());
			else
				properties.load(new FileReader(propertiesFile));
			
			// run application
			EventQueue.invokeLater(new Runnable() {
				public void run() {
					try {
						ParrotApp window = new ParrotApp(dataPath, historyPath, properties);
						window.frame.setLocationRelativeTo(null);
						window.frame.setVisible(true);
					} catch (Exception e) {
						Logger.getLogger("ParrotApp").error(e.getMessage(), e);
					}
				}
			});
			//

        } catch (CmdLineException e) {
        	System.err.print("Usage: Digital Parrot History Pre-Processor");
        	parser.printSingleLineUsage(System.err);
        	System.err.println();
            parser.printUsage(System.err);
            System.exit(64);
        } catch (IllegalArgumentException e) {
        	System.err.println("IllegalArgumentException: " + e.getMessage());
        } catch (FileNotFoundException e) {
        	System.err.println("FileNotFoundException: " + e.getMessage());
        } catch (IOException e) {
        	System.err.println("IOException: " + e.getMessage());
		}
	}

	private ParrotModelWithHistory model;

	/**
	 * Create the application.
	 */
	public ParrotApp(String dataFilename, String historyFilename, Properties properties) {
		LOG.setLevel(Level.ALL);

		LOG.debug("Create ParrotModelWithHistory and load ontologies.");
		model = (ParrotModelWithHistory) ParrotModelFactory.getInstance(Style.GRAPH_WITH_HISTORY).createModel();
		model.loadData("file:" + dataFilename, "file:" + historyFilename);
		
		LOG.debug("Initialise the Application Window.");
		initializeFrame(properties);
	}

	
	private void initializeFrame(Properties properties) {
		
		frame = new ParrotAppFrame(properties);
		
		for (MainViewComponent mainView : frame.getMainViews())
			if (mainView != null)
				mainView.setModel((ParrotModel) model);
		
		for (NavigatorComponent navigator : frame.getNavigators())
			if (navigator != null)
				navigator.setModel((ParrotModel) model);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.validate();
			}
		});
	}

}
