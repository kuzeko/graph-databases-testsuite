package com.graphbenchmark.common.schema;

import java.util.*;

public class Schema {
	public Set<String> vertex_labels = new TreeSet<>();

	// We assume directed graphs
	// edge_label --connects-> (source_label, dst_label);
	public Map<String, Set<List<String>>> edge_labels = new HashMap<>();

	// Vertex label -> <property_name, property_type>
	public Map<String, Map<String, Class>> properties = new HashMap<>();
}
