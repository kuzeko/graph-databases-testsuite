package com.graphbenchmark.common.samples;

import java.util.List;
import java.util.UUID;

public class MappedSamplesSerialized {
	public List<String> nodes;
	public String node_id_class; 		// Will be populated if ther is any node or path.

	public List<String> edges;
	public String edge_id_class;

	public List<String> paths_sources;
	public List<String> paths_targets;

	public UUID sample_id;
}
