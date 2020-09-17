package com.graphbenchmark.common.samples;

import java.util.List;
import java.util.UUID;

public class RawSamples {
	public List<Long> nodes;
	public List<String> node_labels;
	public List<Property> node_props;

	public List<Sample.Edge> edges;
	public List<String> edge_labels;
	public List<Property> edge_props;

	public List<Sample.GPath> paths;

	public Long max_uid;
	public UUID sample_id;
}

