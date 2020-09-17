package com.graphbenchmark.shell.sqlgpg;

import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.umlg.sqlg.structure.SqlgGraph;

import java.util.HashMap;
import java.util.Map;

public class SqlgPgShell extends GenericShell {
	static {
		shellClass = SqlgPgShell.class;
	}

	private static final Boolean lock = false;
	private static SqlgGraph graph = null;

	@Override
	public GraphTraversalSource getConcurrentGTS() {
		// TODO: consider create schema a priory
		// http://sqlg.org/docs/2.0.0-SNAPSHOT/#_topology_eager_creation
		// NOTE: v2.0.0 loaders is broken. Must use >= 2.0.1

		// NOTE: concurrent modification to the schema is broken (blocks)

		// NOTE: must use singleton connection because each time a connection is established,
		// 	the driver issues an update query to a "metatable". This breaks, like for update-transactions,
		// 	when spawning multiple connections.
		//	The error presents itself by transaction failure, and deep down, in "error reading block".
		if (graph == null) {
			synchronized (lock) {
				if (graph == null) {
					Configuration c = new PropertiesConfiguration();
					c.setProperty("jdbc.url", "jdbc:postgresql://localhost:5432/thedb");
					c.setProperty("jdbc.username", "postgres");
					c.setProperty("jdbc.password", "");

					try {
						graph = SqlgGraph.open(c);
					} catch (Exception e) {
						e.printStackTrace();
						GdbLogger.getLogger().fatal("Error creating graph object");
					}
				}
			}
		}

		return graph.traversal();
	}

	@Override
	public GenericIndexMgm getIndexManager(Graph g) {
		return new IndexMgm(g);
	}

	@Override
	public Map<String, String> getQueryOverrideMap() {
		HashMap<String, String> h = new HashMap<>();
		h.put(com.graphbenchmark.queries.mgm.MapSample.class.getName(), MapSample.class.getName());
		return h;
	}
}
