package de.kmamut.parrot.history.changes;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResourceRequiredException;
import com.hp.hpl.jena.vocabulary.OWL2;

import de.kmamut.parrot.history.OntologyModelSettings;

public class ComparableOntology implements Comparable<ComparableOntology> {
	
	private static final Logger LOG = Logger.getLogger(ComparableOntology.class);
	
	private String ontologyFilename = null;
	private OntModel ontologyModel = null;
	private Version version = null;
	
	/**
	 * Creates containers that hold ontologies for comparison and load ontologies from filenames into them.
	 * @param ontologyFilenames List of ontology filenames.
	 * @return List of containers with comparable ontologies.
	 */
	public static List<ComparableOntology> createFromFilenames(List<String> ontologyFilenames) {
		List<ComparableOntology> comparableOntologies = new ArrayList<ComparableOntology>(ontologyFilenames.size());
		for (String ontologyFilename : ontologyFilenames) {
			comparableOntologies.add(createFromFilename(ontologyFilename));
		}
		return comparableOntologies;
	}

	/**
	 * Creates container that hold ontology for comparison and load ontology from filename into it.
	 * @param ontologyFilename Ontology filename.
	 * @return Container with comparable ontology.
	 */
	public static ComparableOntology createFromFilename(String ontologyFilename) {
		if (ontologyFilename == null) 
			throw new IllegalArgumentException("The ontology filename must not be null."); 
		return new ComparableOntology(ontologyFilename);
	}
	
	private static String logTime(long time) {
		return (time / 1000000) + " ms";
	}

	private ComparableOntology(String ontologyFilename) {
		LOG.setLevel(Level.ALL);
		LOG.debug("Create comparable ontology from ontology file: " + ontologyFilename);
		this.ontologyFilename = ontologyFilename;
		long startTime = 0;
		if (LOG.isInfoEnabled()) { startTime = System.nanoTime(); }
		// load ontology from file
		ontologyModel = OntologyModelSettings.createModel();
		ontologyModel.read("file:" + ontologyFilename);
		// extract ontology meta data
		Ontology ontology = ontologyModel.listOntologies().toList().get(0);
		// extract version information
		String ontologyIRI = null, versionIRI = null, priorVersionIRI = null;
		// ontology IRI
		ontologyIRI = ontology.getURI();
		LOG.debug("Ontology IRI: " + ontologyIRI);
		try {
			// version IRI
			versionIRI = ontology.getPropertyValue(OWL2.versionIRI).asResource().getURI();
			LOG.debug("Version IRI: " + versionIRI);
		} catch (NullPointerException e) {
			LOG.debug("No version IRI provided.");
		} catch (ResourceRequiredException e) {
			LOG.error("Version IRI is not a valid resource: " + e.getMessage());
		}
		try {
			// prior version IRI
			priorVersionIRI = ontology.getPriorVersion().asResource().getURI();
			LOG.debug("Proir version: " + priorVersionIRI);
		} catch (NullPointerException e) {
			LOG.debug("No prior version IRI provided.");
		} catch (ResourceRequiredException e) {
			LOG.error("Prior version IRI is not a valid resource: " + e.getMessage());
		}
		// version string
		String versionInfo = ontology.getVersionInfo();
		
		// create version object
		this.version = new Version(versionInfo, ontologyIRI, versionIRI, priorVersionIRI);
		// done
		LOG.info("Ontology v" + version + " loaded. " + logTime(System.nanoTime() - startTime));
		LOG.debug(ontologyModel.listStatements().toList().size() + " statements loaded from " + ontologyFilename);
	}

	public String getFilename() {
		return ontologyFilename;
	}
	
	public Model getOntModel() {
		return ontologyModel;
	}


	// versioned object behaviour
	
	/**
	 * Get the Version of this comparable ontology  
	 * @return The Version this ontology.
	 */
	public Version getVersion() {
		return version;
	}

	@Override
	public int compareTo(ComparableOntology o) {
		return version.compareTo(o.version);
	}

}
