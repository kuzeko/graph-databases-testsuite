package com.graphbenchmark.shell.arangodb;

import com.graphbenchmark.common.GenericIndexMgm;
import org.apache.tinkerpop.gremlin.structure.Graph;

public class IndexMgm extends GenericIndexMgm {

	public IndexMgm(Graph g) {
		super(g);
	}

	// Props are not in the node document, but in ELEMENT-PROPERTIES as (name, value) dict
	// That means that we cannot index one single attribute.
    // In other terms we cannot index on any property.
}
