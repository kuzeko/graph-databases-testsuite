package com.graphbenchmark.shell.neo4j;

import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.GenericIndexMgm;
import org.apache.tinkerpop.gremlin.neo4j.structure.Neo4jGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;

public class IndexMgm extends GenericIndexMgm {
	public IndexMgm(Graph g) {
		super(g);
	}


	public Boolean waitForIndex = true;

	@Override
	public void node(String label, String prop_name) {
		Neo4jGraph ng = (Neo4jGraph) this.g;
		String lbl = label != null & label.length() > 0 ? label : "vertex";

		// we run on 3.4.11
		// https://neo4j.com/docs/cypher-manual/3.5/schema/index/#schema-index-create-a-single-property-index
		ng.cypher(String.format("CREATE INDEX ON :%s(%s)", lbl, prop_name));
		ng.tx().commit();

		// Index creation is async, we may want to wait.
		if (waitForIndex)
			waitForIndex();
	}

	public void waitForIndex() {
		try {
			// This is async and db.awaitIndex() is not in the community edition.
			// We shall wait then just sleep and guess.
			// TODO: consider implementing a polling logic.
			GdbLogger.getLogger().debug("Waiting for the index to build, going to sleep.");
			Thread.sleep(1000 * 60 * 60 * 3);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
