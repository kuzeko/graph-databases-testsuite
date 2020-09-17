package com.graphbenchmark.shell.orientdb;

import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.GenericIndexMgm;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.structure.Graph;

public class IndexMgm extends GenericIndexMgm {
	private OrientdbShell shell;
	public IndexMgm(Graph g, OrientdbShell shell) {
		super(g);
		this.shell = shell;
	}
	// https://orientdb.com/docs/last/Indexes.html

	@Override
	public void node(String label, String prop_name) {
		OrientGraph noTxOg = shell.mustNotTX(this.g);

		// "CREATE INDEX addresses ON Employee (address) NOTUNIQUE METADATA {ignoreNullValues: true}";
		// NOTUNIQUE = SB-Tree Algorithm; these indexes allow duplicate keys.
		String query = "CREATE INDEX %3$s ON `%1$s` (`%2$s`) NOTUNIQUE METADATA {ignoreNullValues: true}";

		String index_name = String.format("%1$s_%2$s_index", label, prop_name).replaceAll("[^A-Za-z0-9]", "_");

		// NOTE: the property must exist in the schema. We cannot create it here as we are missing the type.

		// https://orientdb.com/docs/last/Indexes.html
		// From: OrientGraph:createVertexIndex(label, "V")
		String cls = noTxOg.labelToClassName(label, OClass.VERTEX_CLASS_NAME);

		noTxOg.executeSql(String.format(query, cls, prop_name, index_name));
		noTxOg.commit();
	}
}
