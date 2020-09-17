package com.graphbenchmark.shell.sqlgpg;

import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.GenericIndexMgm;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.umlg.sqlg.structure.PropertyType;
import org.umlg.sqlg.structure.SqlgGraph;
import org.umlg.sqlg.structure.topology.Index;
import org.umlg.sqlg.structure.topology.IndexType;
import org.umlg.sqlg.structure.topology.PropertyColumn;
import org.umlg.sqlg.structure.topology.VertexLabel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

public class IndexMgm extends GenericIndexMgm {
	public IndexMgm(Graph g) {
		super(g);
	}

	@Override
	public void node(String label, String prop_name) {
		// Official documentation example:
		// http://www.sqlg.org/docs/2.0.0-SNAPSHOT/#_indexes

		SqlgGraph sg = (SqlgGraph)this.g;

		// Ensure the actual label/property exists
		VertexLabel label_tbl = sg.getTopology().getPublicSchema()
			.ensureVertexLabelExist(label, new HashMap<>() {{
				put("name", PropertyType.STRING);
			}});

		// How can this ever fail? Check again anyway.
		Optional<PropertyColumn> property_column = label_tbl.getProperty(prop_name);
		if (property_column.isEmpty()) {
			GdbLogger.getLogger().fatal("Cannot create index on non initialized label:property -> %s:%s", label, prop_name);
			return; // (Will never be invoked due to previous fatal)
		}

		// Create the index.
		Index index = label_tbl.ensureIndexExists(IndexType.NON_UNIQUE, Collections.singletonList(property_column.get()));
		sg.tx().commit();
	}
}
