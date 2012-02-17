package de.kmamut.parrot.history;

import java.util.Map;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.mindswap.pellet.PelletOptions;
import org.mindswap.pellet.PelletOptions.MonitorType;
import org.mindswap.pellet.jena.PelletReasonerFactory;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.TypeMapper;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.kmamut.parrot.history.changes.Change;
import de.kmamut.parrot.history.changes.ChangeSet;
import de.kmamut.parrot.history.changes.ChangeType;
import de.kmamut.parrot.history.changes.Version;

public class HistoryModelHelper extends OntologyModelSettings {
	
	private final static Logger LOG = Logger.getLogger(HistoryModelHelper.class);
	
	
	// creation of OntModel for HistroyModel 
	
	private final static String conceptsURI = uriPrefix + "History/2011/11/History.owl#";
	
	/**
	 * Creates an OntModel suitable to store information from a HistoryModel.
	 * @return OntModel as required by HistoryModel.
	 */
	public static OntModel createHistoryModel() {
		OntModel historyModel = (OntModel) createModelSpec(URL_MAP).doCreateModel();
		historyModel.setNsPrefixes(PREFIX_MAP);
		historyModel.setNsPrefix("history", conceptsURI);
		return historyModel;
	}
	
	/**
	 * Creates an empty OntModel for ParrotModel data.
	 * @return OntModel as required by ParrotModel (with Pellet reasoner).
	 */
	public static OntModel createEmptyParrotModel() {
		PelletOptions.USE_CLASSIFICATION_MONITOR = MonitorType.NONE;
		return (OntModel) createModelSpec(PelletReasonerFactory.THE_SPEC, URL_MAP).doCreateModel().removeAll();
	}
	
	private static final Map<String, String> URL_MAP = createHistoryUrlMap();
	private static Map<String, String> createHistoryUrlMap() {
		OntologyUrlMap map = createParrotUrlMap();
		map.put(conceptsURI, "History.owl");
		return map;
	}
	
	
	// creation of concepts for OntModel of HistroyModel
	
	private static final OntModel historyConcepts = loadHistoryConceptModel();
	private static OntModel loadHistoryConceptModel() {
    	OntModel historyConceptModel = createHistoryModel();
    	historyConceptModel.read(conceptsURI);
    	historyConceptModel.loadImports();
    	return historyConceptModel;
    }
    
	private static OntClass conceptClass(String name) {
		return historyConcepts.getOntClass(conceptsURI + name);
	}
	
	private static OntProperty conceptProperty(String name) {
		return historyConcepts.getOntProperty(conceptsURI + name);
	}
	
	private static String getURI(String name) {
		return uriPrefix.replaceFirst("Terms", "Facts") + "history.rdf#" + name;
	}
	
	
	// creation of history Individuals from history Objects (Version, Change, ChangeSet)
	
	private static int getHash(String[] strings) {
		String string = "";
		for (String s : strings) { if (null != s) { string += s; } }
		return string.hashCode();
	}
	
	private static Individual createVersion(OntModel historyModel, Version version) {
		String uri = getURI("version-" + version.toString());
		Individual individual = historyModel.getIndividual(uri);
		if (individual != null)
			return individual;
		individual = historyModel.createIndividual(uri, conceptClass("Version"));
		individual.setVersionInfo(version.getVersionInfo());
		if (version.hasOntologyIRI())
			individual.addProperty(conceptProperty("ontologyIRI"), version.getOntologyIRI());
		if (version.hasVersionIRI())
			individual.addProperty(conceptProperty("versionIRI"), version.getVersionIRI());
		if (version.hasPriorVersionIRI())
			individual.addProperty(conceptProperty("priorVersionIRI"), version.getPriorVeresionIRI());
		return individual;
	}
	
	private static Individual createChange(OntModel historyModel, Change change) {
		if (change.getType().equals(ChangeType.UNTOUCHED))
			return null;
		String uri = getURI(change.getType().toIndividualName() + getHash(new String[]{ 
				change.getSubjectURI(), change.getPredicateURI(), change.getOldObjectValue(), change.getNewObjectValue() }));
		Individual individual = historyModel.getIndividual(uri);
		if (null != individual) return individual;
		individual = historyModel.createIndividual(uri, conceptClass(change.getType().toOntClassName()));
		individual.addProperty(conceptProperty("subject"), change.getSubjectURI());
		individual.addProperty(conceptProperty("predicate"), change.getPredicateURI());
		if (change.isLiteralChange()) {
			if (change.isLabelChange()) {
				String language = change.getLanguage();
				if (null != change.getOldObjectValue())
					individual.addProperty(conceptProperty("oldObject"), change.getOldObjectValue(), language);
				if (null != change.getNewObjectValue())
					individual.addProperty(conceptProperty("newObject"), change.getNewObjectValue(), language);
			} else {
				String datatypeURI = change.getDatatypeURI();
				RDFDatatype objectDatatype = TypeMapper.getInstance().getSafeTypeByName(datatypeURI);
				if (datatypeURI == null || datatypeURI.isEmpty())
					datatypeURI = RDFS.Literal.getURI();
				if (null != change.getOldObjectValue())
					individual.addProperty(conceptProperty("oldObject"), change.getOldObjectValue(), objectDatatype);
				if (null != change.getNewObjectValue())
					individual.addProperty(conceptProperty("newObject"), change.getNewObjectValue(), objectDatatype);
			}
		} else {
			if (null != change.getOldObjectValue())
				individual.addProperty(conceptProperty("oldObject"), change.getOldObjectValue());
			if (null != change.getNewObjectValue())
				individual.addProperty(conceptProperty("newObject"), change.getNewObjectValue());
		}
		return individual;
	}
	
	private static Individual createChangeSet(OntModel historyModel, ChangeSet changeSet) { //Map<ChangeType, Set<Individual>> changeIndividuals, Individual oldVersion, Individual newVersion) {
		Individual oldVersion = createVersion(historyModel, changeSet.getPriorVersion());
		Individual newVersion = createVersion(historyModel, changeSet.getVersion());
		
		String uri = getURI("changeset-" + getHash(new String[] { oldVersion.getURI(), newVersion.getURI() }));
		Individual individual = historyModel.getIndividual(uri);
		if (null != individual) return individual;
		individual = historyModel.createIndividual(uri, conceptClass("ChangeSet"));

		for (Change change : changeSet.getAll())
			switch (change.getType()) {
			case ADDITION:
				individual.addProperty(conceptProperty("hasAddition"), createChange(historyModel, change));
				break;
			case REMOVAL:
				individual.addProperty(conceptProperty("hasRemoval"), createChange(historyModel, change));
				break;
			case CHANGE:
				individual.addProperty(conceptProperty("hasChange"), createChange(historyModel, change));
				break;
			default:
				LOG.error("Unexpected change of type " + change.getType().toString() + ".");
			}
			
		individual.addProperty(conceptProperty("fromVersion"), oldVersion);
		individual.addProperty(conceptProperty("toVersion"), newVersion);
		
		oldVersion.addProperty(conceptProperty("nextVersion"), newVersion);
		newVersion.addProperty(conceptProperty("priorVersion"), oldVersion);
		oldVersion.addProperty(conceptProperty("upGradeWith"), individual);
		newVersion.addProperty(conceptProperty("downGradeWith"), individual);
		
		return individual;
	}
	
	
	// extraction of history Objects (Version, Change, ChangeSet) from Individuals

	private static Version extractVersion(Individual individual) {
		String versionString = individual.getLocalName().replaceFirst("version-", "");
		String ontologyIRI;
		try {
			ontologyIRI = individual.getPropertyValue(conceptProperty("ontologyIRI")).asLiteral().getString();
		} catch (NullPointerException e) {
			ontologyIRI = null;
		}
		String versionIRI;
		try {
			versionIRI = individual.getPropertyValue(conceptProperty("versionIRI")).asLiteral().getString();
		} catch (NullPointerException e) {
			versionIRI = null;
		}
		String priorVersionIRI;
		try {
			priorVersionIRI = individual.getPropertyValue(conceptProperty("priorVersionIRI")).asLiteral().getString();
		} catch (NullPointerException e) {
			priorVersionIRI = null;
		}
		String versionInfo = individual.getVersionInfo();;
		if (versionInfo == null || !versionInfo.contains(versionString))
			versionInfo = versionString;
		return new Version(versionInfo, ontologyIRI, versionIRI, priorVersionIRI);
	}

	private static Change extractChange(Individual changeIndividual) {
		String subjectURI = changeIndividual.getPropertyValue(conceptProperty("subject")).asLiteral().getLexicalForm();
		String predicateURI = changeIndividual.getPropertyValue(conceptProperty("predicate")).asLiteral().getLexicalForm();
		RDFNode oldObjectNode = changeIndividual.getPropertyValue(conceptProperty("oldObject"));
		String oldObjectValue = (oldObjectNode == null) ? null : oldObjectNode.asLiteral().getLexicalForm();
		RDFNode newObjectNode = changeIndividual.getPropertyValue(conceptProperty("newObject"));
		String newObjectValue = (newObjectNode == null) ? null : newObjectNode.asLiteral().getLexicalForm();
		String datatypeURI = (newObjectNode == null) ? oldObjectNode.asLiteral().getDatatypeURI() : newObjectNode.asLiteral().getDatatypeURI();
		if ((datatypeURI != null && !datatypeURI.isEmpty()) || predicateURI.equals(RDFS.label.getURI())) {
			String language = (newObjectNode == null) ? oldObjectNode.asLiteral().getLanguage() : newObjectNode.asLiteral().getLanguage();
			return new Change(subjectURI, predicateURI, oldObjectValue, newObjectValue, datatypeURI, language);
		} else
			return new Change(subjectURI, predicateURI, oldObjectValue, newObjectValue);
	}
	
	private static ChangeSet extractChangeSet(Individual changeSetIndividual) {
		Version fromVersion = extractVersion(changeSetIndividual.getPropertyResourceValue(conceptProperty("fromVersion")).as(Individual.class));
		Version toVersion = extractVersion(changeSetIndividual.getPropertyResourceValue(conceptProperty("toVersion")).as(Individual.class));
		ChangeSet changeSet = ChangeSet.createEmptyChangeSet(fromVersion, toVersion);
		for (RDFNode changeIndividual : changeSetIndividual.listPropertyValues(conceptProperty("hasAddition")).toSet())
			changeSet.addAddition(extractChange(changeIndividual.as(Individual.class)));
		for (RDFNode changeIndividual : changeSetIndividual.listPropertyValues(conceptProperty("hasRemoval")).toSet())
			changeSet.addRemoval(extractChange(changeIndividual.as(Individual.class)));
		for (RDFNode changeIndividual : changeSetIndividual.listPropertyValues(conceptProperty("hasChange")).toSet())
			changeSet.addChange(extractChange(changeIndividual.as(Individual.class)));
		return changeSet;
	}


	// update HistoryModel with ChangeSets
	
	/**
	 * Updates the historyModel with changes from changeSets.
	 * @param historyModel The HistoryModel that will be updated.
	 * @param changeSets The ChangeSets that will be added to HistroyModel.
	 */
	public static void updateChangeSets(OntModel historyModel, TreeSet<ChangeSet> changeSets) {
		for (ChangeSet changeSet : changeSets)
			createChangeSet(historyModel, changeSet);
	}
	
	
	// load ChangeSets from HistoryModel

	/**
	 * Load ChangeSets from historyModel.
	 * @param historyModel The HistoryModel that the ChangeSets will be read from.
	 * @return Set of ChangeSet sorted by Version. 
	 */
	public static TreeSet<ChangeSet> loadChangeSets(OntModel historyModel) {
		TreeSet<ChangeSet> changeSets = new TreeSet<ChangeSet>();
		for (Individual changeSetIndividual : historyModel.listIndividuals(conceptClass("ChangeSet")).toSet())
			changeSets.add(extractChangeSet(changeSetIndividual));
		return changeSets;
	}

}
