package de.kmamut.parrot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import de.kmamut.parrot.history.HistoryModel;
import de.kmamut.parrot.history.changes.ChangeSet;
import de.kmamut.parrot.history.changes.ComparableOntology;

class HistoryModelUpdaterSettings {
	@Option(name = "--histroy-file", aliases = {"-h"}, usage = "History that stores changes between ontology versions. If not specified history.rdf is used.")
	public String historyFilename =  "./history.rdf";
	@Argument(required = true, multiValued = true, metaVar = "filenames", usage = "Ontlogies version that will be compared. It is expected that the alphabethical order of filenames equals the version order.")
	public List<String> ontologyFilenames;  
}

public class HistoryModelUpdater {
	
	private static final Logger LOG = Logger.getLogger(HistoryModelUpdater.class);
	
	private static void UpdateHistory(List<String> ontologyFilenames, String historyFilename) {
		LOG.setLevel(Level.ALL);
		LOG.info("Compare ontologies...");
		List<ChangeSet> changeSets = ChangeSet.compare(ComparableOntology.createFromFilenames(ontologyFilenames));
		LOG.info("Load and update history...");
		HistoryModel.load(historyFilename).update(new TreeSet<ChangeSet>(changeSets)).write();
	}

	public static void main (String[] args)
    {
		HistoryModelUpdaterSettings settings = new HistoryModelUpdaterSettings();
		CmdLineParser parser = new CmdLineParser(settings);
        try {
            parser.parseArgument(args);
            if (!(settings.ontologyFilenames.size() > 1))
            	throw new IllegalArgumentException("Wrong number of aruments. At least two Ontology Filenames are required."); 
            for (String filename : settings.ontologyFilenames)
            	if (!(new File(filename)).exists())
            		throw new FileNotFoundException(filename);
            UpdateHistory(settings.ontologyFilenames, settings.historyFilename);
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
        }
    }
}
