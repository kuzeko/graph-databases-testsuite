package com.graphbenchmark.shell.arangodb;

// https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider

import com.arangodb.tinkerpop.gremlin.utils.ArangoDBConfigurationBuilder;
import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.schema.QSupportSchema;
import com.graphbenchmark.common.schema.v3.Schema;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.util.GraphFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ArangoDbShell extends GenericShell {
	static {
		shellClass = ArangoDbShell.class;
	}

	final public static String GRAPH_NAME  = "MAIN_GRAPH";


	BaseConfiguration configure() {
		// https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/58
		// NOTE: this actually creates the collections in the db.

		Schema.SchemaStream ss = (new Schema(this.runConf.dataset.path)).getSchemaStream();

		ArangoDBConfigurationBuilder builder = new ArangoDBConfigurationBuilder();

		Set<String> nodes = ss.node_labels.collect(Collectors.toSet());

		// Build base schema
		builder.graph(GRAPH_NAME);

		nodes.add(QSupportSchema.VLabel);
		nodes.forEach(builder::withVertexCollection);

		ss.edge_labels.forEach(lbl -> {
			builder.withEdgeCollection(lbl);
			builder.configureEdge(lbl, nodes, nodes);
		});

		ss.close();

		// Add framework fixtures labels
		builder.withEdgeCollection(QSupportSchema.ELabel);
		builder.configureEdge(QSupportSchema.ELabel, nodes, nodes);

		return builder.build();
	}


	@Override
	public GraphTraversalSource getConcurrentGTS() {
		return GraphFactory.open(this.configure()).traversal();
	}

	// TODO: why in the doc they use gts.clone() ??
	// NOTE: cannot set id, only generated: providing a TinkerPop vertex id (T.id) actually sets the vertex's ArangoDB name.


	/* NOTE: On transaction
		Source: https://github.com/ArangoDB-Community/arangodb-tinkerpop-provider/issues/20

		Currently all operations are translated into AQL and executed server side. As a result all operations
	     are atomic and transactions are not supported. Although some bridge to JavaScript is plausible,
	     making this work is very difficult. Any operation will have to query both the db directly and the
	     "temporal store" of uncommitted information. Another option like loading the graph into memory
	     fails for large graphs.

		The lack of transactions is a limitation of how the Java Driver works and overcoming this will result
		 in half baked solutions.
	 */

	@Override
	public GenericIndexMgm getIndexManager(Graph g) {
		return new IndexMgm(g);
	}

	@Override
	public Map<String, String> getQueryOverrideMap() {
		HashMap<String, String> h = new HashMap<>();
		h.put(com.graphbenchmark.queries.mgm.Load.class.getName(), Load.class.getName());
		return h;
	}
}
