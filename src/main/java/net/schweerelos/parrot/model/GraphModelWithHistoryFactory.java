package net.schweerelos.parrot.model;

import com.hp.hpl.jena.ontology.OntModel;

public class GraphModelWithHistoryFactory extends ParrotModelFactory {

	private OntModel model;

	public GraphModelWithHistoryFactory(OntModel model) {
		this.model = model;
	}

	@Override
	public ParrotModel createModel() {
		return new GraphParrotModelWithHistory(model);
	}

}
