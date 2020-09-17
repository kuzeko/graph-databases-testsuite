package com.graphbenchmark.common.samples;

import com.graphbenchmark.common.GdbLogger;

import java.util.*;

public class Sample {
	public RawSamples raw;
	public MappedSamples mapping;
	public UUID sample_id;

	public NodeIdResolver nresovler;

	public void index() {
		this.nresovler = new NodeIdResolver(this.raw, this.mapping);
	}

	public static class Edge {
		public Long source, target;
		public String label;

		public Edge(Long source, Long target, String label) {
			this.source = source;
			this.target = target;
			this.label = label;
		}
	}


	// Nodes mappings can be accessed by index in the array or with this reverse index
	public static class NodeIdResolver {
		private final HashMap<Long, Object> raw_to_mapped = new HashMap<>();
		private final HashMap<Object, Long> mapped_to_raw = new HashMap<>();



		public NodeIdResolver(RawSamples raw, MappedSamples mapping) {

			// Sampled nodes
			for (int i=0; i<raw.nodes.size(); i++) {
				this.raw_to_mapped.put(raw.nodes.get(i), mapping.nodes.get(i));
				this.mapped_to_raw.put(mapping.nodes.get(i), raw.nodes.get(i));
			}

			// Sampled paths
			for (int i=0; i<raw.paths.size(); i++) {
				this.raw_to_mapped.put(raw.paths.get(i).source_id, mapping.paths.get(i).source_id);
				this.raw_to_mapped.put(raw.paths.get(i).target_id, mapping.paths.get(i).target_id);

				this.mapped_to_raw.put(mapping.paths.get(i).source_id, raw.paths.get(i).source_id);
				this.mapped_to_raw.put(mapping.paths.get(i).target_id, raw.paths.get(i).target_id);
			}

		}

		public Object r2m(Long id) {
			return this.raw_to_mapped.get(id);
		}

		public Long m2r(Object id) {
			return this.mapped_to_raw.get(id);
		}
	}

	public static class GPath {
		public long source_id, target_id;
		public String source_lbl, target_lbl;
		public List<String> sequence = new ArrayList<>();

		@Override
		public String toString() {
			return "GPath{" +
					"source_id=" + source_id +
					", target_id=" + target_id +
					", source_lbl='" + source_lbl + '\'' +
					", target_lbl='" + target_lbl + '\'' +
					", sequence=" + sequence +
					'}';
		}
	}
}


