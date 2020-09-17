package com.graphbenchmark.shell.janus;

import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.janusgraph.core.JanusGraphFactory;

import java.util.HashMap;
import java.util.Map;

// TODO: CQL backend

public class JanusShell extends GenericShell {
	static {
		shellClass = JanusShell.class;
	}

	@Override
	public GraphTraversalSource getConcurrentGTS() {

		try {
			/* NOTE: to use "schema.default=none" we need also to create the properties
			*	--> requires schema.v3
			*/
			Configuration c = new PropertiesConfiguration(getClass().getClassLoader().getResource("conf/cassandra_es.properties"));
			return JanusGraphFactory.open(c).traversal();
		} catch (ConfigurationException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Failed to create graph object");
		}
		return null;
	}

	@Override
	public GenericIndexMgm getIndexManager(Graph g) {
		return new IndexMgm(g);
	}

	@Override
	public Map<String, String> getQueryOverrideMap() {
		HashMap<String, String> h = new HashMap<>();
		h.put(com.graphbenchmark.queries.mgm.Load.class.getName(), Load.class.getName());
		h.put(com.graphbenchmark.queries.mgm.MapSample.class.getName(), MapSample.class.getName());
		return h;
	}
}
