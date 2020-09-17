package com.graphbenchmark.common.samples;

import java.util.List;
import java.util.UUID;

public class MappedSamples {
	public List<Object> nodes;
	public List<Object> edges;

	public List<GPath> paths;

	public UUID sample_id;

	public static class GPath {
		public Object source_id, target_id;

		public GPath() {
		}

		public GPath(Object source_id, Object target_id) {
			this.source_id = source_id;
			this.target_id = target_id;
		}
	}
}
