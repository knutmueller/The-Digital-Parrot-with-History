package net.schweerelos.parrot.model;

import java.util.TreeMap;

import com.hp.hpl.jena.ontology.OntModel;

import de.kmamut.parrot.history.HistoryModel;
import de.kmamut.parrot.history.changes.Version;

public interface ParrotModelWithHistory extends ParrotModel {
	
	@Override
	@Deprecated
	/**
	 * Do not use this method. Use loadData(String datafile, String historyfile) instead.
	 */
	public void loadData(String datafile);
	
	/**
	 * Initializes the data model from datafile and the histroy model from historyfile. 
	 * @param datafile File containing RDF memory data in a format readable by Jena.
	 * @param historyfile File containing RDF history data in a format readable by Jena.
	 */
	public void loadData(String datafile, String historyfile);

	/**
	 * Get the HistoryModel that holds all history information.
	 * @return HistoryModel Object.
	 */
	public HistoryModel getHistoryModel();

	/**
	 * Get OntModel for each version.
	 * @return Map from version to corresponding OntModel.
	 */
	public TreeMap<Version, OntModel> getVersionedOntModels();

}
