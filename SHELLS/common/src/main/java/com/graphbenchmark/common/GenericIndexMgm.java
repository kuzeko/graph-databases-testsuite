package com.graphbenchmark.common;


import org.apache.commons.lang.NotImplementedException;
import org.apache.tinkerpop.gremlin.structure.Graph;

import java.util.Collection;

public class GenericIndexMgm {
	public Graph g;

	public GenericIndexMgm(Graph g) {
		this.g = g;
	}

	public boolean labelSensitive() {
		return true;
	}

	public void node(String label, String prop_name) {
		throw new NotImplementedException();
	}

	public void dropNode(String label, String prop_name) {
		throw new NotImplementedException();
	}

	public void edge(String label, String prop_name) {
		throw new NotImplementedException();
	}
}
