package com.graphbenchmark.common;

import com.graphbenchmark.common.samples.Sample;

public class ExperimentSettings extends BaseQParam {
	public int nodes, node_labels, node_props,
			   edges, edge_labels, edge_props,
			   paths, paths_max_len,
			   threads=1;

	public static ExperimentSettings infer(Sample s) {
		ExperimentSettings e = new ExperimentSettings();
		if (s == null || s.raw == null)
			return e;

		e.nodes = s.raw.nodes.size();
		e.node_labels = s.raw.node_labels.size();
		e.node_props = s.raw.node_props.size();

		e.edges = s.raw.edges.size();
		e.edge_labels = s.raw.edge_labels.size();
		e.edge_props = s.raw.edge_props.size();

		e.paths = s.raw.paths.size();
		e.paths_max_len = s.raw.paths.stream().mapToInt(p -> p.sequence.size()).max().orElse(0);

		return e;
	}
}
