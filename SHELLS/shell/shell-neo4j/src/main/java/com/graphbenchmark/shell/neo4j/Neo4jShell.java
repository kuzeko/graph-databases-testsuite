package com.graphbenchmark.shell.neo4j;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.HashMap;
import java.util.Map;


public class Neo4jShell extends GenericShell {
    static {
        shellClass = Neo4jShell.class;
    }

    static final Boolean l = false;
    static Graph g = null;
    static GraphTraversalSource t = null;

    static final String db_path = "/data/";

    @Override
    public GraphTraversalSource getConcurrentGTS() {
        // See https://github.com/neo4j-contrib/neo4j-tinkerpop-api-impl/issues/19
        if (g == null) {
            synchronized (l) {
                if (g == null) {
                	g = Neo4jGraph.open(db_path);
                	t = g.traversal();
				}
            }
        }

        return t;
    }

    @Override
    public GenericIndexMgm getIndexManager(Graph g) {
        return new IndexMgm(g);
    }


    public Map<String, String> getQueryOverrideMap() {
        HashMap<String, String> h = new HashMap<>();
        h.put(com.graphbenchmark.queries.mgm.IndexUID.class.getName(), IndexUID.class.getName());
        return h;
    }
}



