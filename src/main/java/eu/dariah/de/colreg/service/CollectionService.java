package eu.dariah.de.colreg.service;

import java.util.List;

import eu.dariah.de.colreg.model.Agent;
import eu.dariah.de.colreg.model.Collection;

public interface CollectionService {
	public Collection createCollection();
	public void save(Collection c);
	public Collection findCurrentByCollectionId(String id);
	public List<Collection> findAllCurrent();
	public void initializeAgentRelations(Collection c);
	public List<Collection> queryCollections(String query, List<String> excl);
}
