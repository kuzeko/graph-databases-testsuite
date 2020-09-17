package com.graphbenchmark.shell.janus;

import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.GenericIndexMgm;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.SchemaAction;
import org.janusgraph.core.schema.SchemaStatus;
import org.janusgraph.graphdb.database.management.ManagementSystem;

import java.util.concurrent.ExecutionException;

public class IndexMgm extends GenericIndexMgm {
	public IndexMgm(Graph g) {
		super(g);
	}

	@Override
	public boolean labelSensitive() {
		return false;
	}

	@Override
	public void node(String label, String prop_name) {
		// Code imported from official doc:
		// https://docs.janusgraph.org/index-management/index-performance/

		JanusGraph jg = (JanusGraph)this.g;
		String idx_name = String.format("%s_%s_vidx", label, prop_name);

		jg.tx().rollback(); //Never create new indexes while a transaction is active
		JanusGraphManagement mgmt = jg.openManagement();
		PropertyKey propertyKey = mgmt.getPropertyKey(prop_name);
		mgmt.buildIndex(idx_name, Vertex.class).addKey(propertyKey).buildCompositeIndex();
		mgmt.commit();

		//Wait for the index to become available
		try {
			ManagementSystem.awaitGraphIndexStatus(jg, idx_name).call();
		} catch (InterruptedException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Crashed while waiting for index %s", idx_name);
		}

		//Reindex the existing data
		mgmt = jg.openManagement();
		try {
			mgmt.updateIndex(mgmt.getGraphIndex(idx_name), SchemaAction.REINDEX).get();
		} catch (InterruptedException|ExecutionException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Crashed while waiting for re-index %s", idx_name);
		}
		mgmt.commit();
	}

	@Override
	public void dropNode(String label, String prop_name) {
		// https://stackoverflow.com/questions/45273709/how-to-remove-janusgraph-index

		JanusGraph jg = (JanusGraph)this.g;
		String idx_name = String.format("%s_%s_vidx", label, prop_name);

		jg.tx().rollback(); //Never create new indexes while a transaction is active
		JanusGraphManagement mgmt = jg.openManagement();

		// Disable the "name" composite index
		JanusGraphIndex nameIndex = mgmt.getGraphIndex(idx_name);
		try {
			mgmt.updateIndex(nameIndex, SchemaAction.DISABLE_INDEX).get();
		} catch (InterruptedException|ExecutionException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Crashed while disabling index %s", idx_name);
		}
		mgmt.commit();
		jg.tx().commit();

		// Block until the SchemaStatus transitions from INSTALLED to REGISTERED
		try {
			ManagementSystem.awaitGraphIndexStatus(jg, idx_name).status(SchemaStatus.DISABLED).call();
		} catch (InterruptedException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Crashed waiting for disabling index %s", idx_name);
		}

		// Delete the index using JanusGraphManagement
		mgmt = jg.openManagement();
		JanusGraphIndex delIndex = mgmt.getGraphIndex(idx_name);
		mgmt.updateIndex(delIndex, SchemaAction.REMOVE_INDEX);
		mgmt.commit();
		jg.tx().commit();
	}
}
