package com.graphbenchmark.common.samples;

import com.google.gson.Gson;
import com.graphbenchmark.common.ExperimentSettings;
import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.Property;

import javax.crypto.spec.GCMParameterSpec;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.graphbenchmark.settings.Venv.MAPPINGS_DIR;
import static com.graphbenchmark.settings.Venv.SAMPLES_DIR;

public class SampleManger {
	private static final GdbLogger log = GdbLogger.getLogger();

	// Performs sampling (or loads them). Sample will contain the actual sample.
	public static Collection<TimingCheckpoint> sampling(final GraphTraversalSource gts, ExperimentSettings es, final Dataset ds, Sample sample) {
		Collection<TimingCheckpoint> tmc = new ArrayList<>();
		sample.raw = create_or_load_sample_file(gts, es, ds, sample.sample_id, tmc);
		sample.mapping = create_mapping_file(gts, sample.raw, ds, sample.sample_id, tmc);
		return tmc;
	}

	public static RawSamples create_or_load_sample_file(final GraphTraversalSource gts, ExperimentSettings es, final Dataset ds, final UUID sample_id, Collection<TimingCheckpoint> tmc) {

		Gson gson = new Gson();
		long start;
		RawSamples raw = null;

		File f = samplePath(ds.path, sample_id).toFile();
		if (!f.exists()) {
			if (f.isDirectory()) {
				log.fatal("Raw sample file is a directory!");
			}

			start = System.currentTimeMillis();
			raw = sample(gts, es, ds, sample_id);
			tmc.add(new TimingCheckpoint(System.currentTimeMillis() - start, "Sampling"));

			try {
				// Create folder if it does not exists
				File folder = samplePath(ds.path, sample_id).getParent().toFile();
				if (!folder.isDirectory() && !folder.mkdirs()) {
					throw new IOException();
				}

				FileWriter fw = new FileWriter(f);
				fw.write(gson.toJson(raw));
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
				log.fatal("Failed to save raw sample file");
			}
		} else {
			try {
				raw = gson.fromJson(Files.readString(samplePath(ds.path, sample_id)), RawSamples.class);
				assert raw != null;
			} catch (IOException e) {
				e.printStackTrace();
				log.fatal("Error reading raw sample");
			}

		}
		return raw;
	}

	public static MappedSamples create_mapping_file(final GraphTraversalSource gts, RawSamples raw, final Dataset ds, final UUID sample_id, Collection<TimingCheckpoint> tmc) {
		Gson gson = new Gson();
		long start;

		start = System.currentTimeMillis();
		MappedSamples mapped = map(gts, ds, raw);
		tmc.add(new TimingCheckpoint(System.currentTimeMillis()-start, "Mapping"));
		File f = mappingPath(sample_id).toFile();
		try {
			// Create folder if it does not exists
			File folder = mappingPath(sample_id).getParent().toFile();
			if (!folder.isDirectory() && !folder.mkdirs()) {
				throw new IOException();
			}

			MappedSamplesSerialized m = new MappedSamplesSerialized();
			m.sample_id = mapped.sample_id;
			m.nodes = mapped.nodes.stream().map(Object::toString).collect(Collectors.toList());
			m.edges = mapped.edges.stream().map(Object::toString).collect(Collectors.toList());

			m.paths_sources = mapped.paths.stream().map(p -> p.source_id).map(Object::toString).collect(Collectors.toList());
			m.paths_targets = mapped.paths.stream().map(p -> p.target_id).map(Object::toString).collect(Collectors.toList());

			if (mapped.nodes.size() > 0) {
				m.node_id_class = mapped.nodes.get(0).getClass().getName();
			}

			if (mapped.edges.size() > 0) {
				m.edge_id_class = mapped.edges.get(0).getClass().getName();
			}

			if (mapped.paths.size() > 0) {
				m.node_id_class = mapped.paths.get(0).source_id.getClass().getName();
			}

			FileWriter fw = new FileWriter(f);
			fw.write(gson.toJson(m));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			log.fatal("[FATAL] Failed to save mapping file");
		}

		return mapped;
	}

	public static Long forceLong(Object o) {
		if (o instanceof Long) {
			return (Long) o;
		}
		if (o instanceof Integer) {
			return ((Integer) o).longValue();
		}
		return Long.valueOf(o.toString());
	}

	public static RawSamples sample(final GraphTraversalSource g, final ExperimentSettings es, final Dataset ds, final UUID sample_id) {
		log.debug("Start sampling");
		Random r = new Random(); //TODO: Why should set the seed somewhere!!!!!!!!

		RawSamples s = new RawSamples();
		s.sample_id = sample_id;
		s.max_uid = forceLong(g.V().values(ds.uid_field).max().next());

		List<Object> raw_nodes_id = new ArrayList<>();
		s.nodes = g.V().sample(es.nodes).toStream()
				.map(v -> {
					raw_nodes_id.add(v.id());
					return v.property(ds.uid_field).value();
				})
				.map(SampleManger::forceLong)
				.collect(Collectors.toList());
		s.node_labels = g.V().label().dedup().sample(es.node_labels).toList();

		if (s.node_labels.isEmpty()) {
			s.node_props = List.of();
			if (es.node_props > 0) {
				log.debug("No node_labels available, not sampling node_props");
			}
		} else {
			HashSet<com.graphbenchmark.common.samples.Property> p = new HashSet<>();

			List<String> nprop_src = new ArrayList<>(s.node_labels);
			for (int i = 0; i < es.node_props * 3 && p.size() < es.node_props && !nprop_src.isEmpty(); i++) {
				String label = nprop_src.get(r.nextInt(nprop_src.size()));
				log.debug("Sampling prop for node label: %s", label);

				Vertex v = g.V().hasLabel(label).sample(1).next();
				log.debug("Vertex: %s", v);

				List<List<String>> props = new ArrayList<>();
				for (Iterator<VertexProperty<Object>> iter = v.properties(); iter.hasNext(); ) {
					VertexProperty<Object> vp = iter.next();
					log.debug("Property (%s): %s [%s]", vp.key(), vp, vp.value().getClass().getName());
					props.add(List.of(vp.key(), vp.value().getClass().getName()));
				}
				log.debug("Found %d props for node %s: %s", props.size(), label, props);

				// If this label has at least one property get it, otherwise try again.
				if (props.isEmpty()) {
					log.debug("[2] No props for %s. Trying with a different one.", label);
					nprop_src.remove(label); 	// NOTE: We assume some sort of labels based schema.
					continue;
				}

				List<String> prop = (new ArrayList<>(props)).get(r.nextInt(props.size()));
				p.add(new com.graphbenchmark.common.samples.Property(label, prop.get(0), prop.get(1)));
				log.debug("Sampled prop: %s/%s", label, prop);
			}
			s.node_props = new ArrayList<>(p);
		}

		log.debug("Looking for %d edges", es.edges);
		s.edges = g.E().sample(es.edges).map(edge -> new Sample.Edge(
				forceLong(edge.get().outVertex().value(ds.uid_field)),
				forceLong(edge.get().inVertex().value(ds.uid_field)),
				edge.get().label()
		)).toList();
		log.debug("Sampled %d edges", s.edges.size());

		log.debug("Looking for %d edge labels", es.edge_labels);
		s.edge_labels = g.E().label().dedup().sample(es.edge_labels).toList();
		log.debug("Sampled %d edge labels", s.edges.size());

		if (s.edge_labels.isEmpty()) {
			s.edge_props = List.of();
			if (es.edge_props > 0)
				log.debug("No edge_labels available, not sampling edge_props");
		} else {
			HashSet<com.graphbenchmark.common.samples.Property> p = new HashSet<>();

			List<String> eprop_src = new ArrayList<>(s.edge_labels);
			for (int i = 0; i < es.edge_props * 3 && p.size() < es.edge_props && !eprop_src.isEmpty(); i++) {
				String label = eprop_src.get(r.nextInt(eprop_src.size()));
				log.debug("Sampling prop for edge label: %s", label);

				org.apache.tinkerpop.gremlin.structure.Edge e = g.E().hasLabel(label).sample(1).next();
				log.debug("Edge: %s", e);

				List<List<String>> props = new ArrayList<>();
				for (Iterator<Property<Object>> iter = e.properties(); iter.hasNext(); ) {
					Property<Object> ep = iter.next();
					log.debug("Property (%s): %s [%s]", ep.key(), ep, ep.value().getClass().getName());
					props.add(List.of(ep.key(), ep.value().getClass().getName()));
				}
				log.debug("Found %d props for edge %s: %s", props.size(), label, props);

				// If this label has at least one property get it, otherwise try again.
				if (props.isEmpty()) {
					log.debug("[2] No props for %s. Trying with a different one.", label);
					eprop_src.remove(label); 	// NOTE: We assume some sort of labels based schema.
					continue;
				}

				List<String> prop = (new ArrayList<>(props)).get(r.nextInt(props.size()));
				p.add(new com.graphbenchmark.common.samples.Property(label, prop.get(0), prop.get(1)));
				log.debug("Sampled prop: %s/%s", label, prop);
			}
			s.edge_props = new ArrayList<>(p);
		}

		//log.debug("Sampling %d paths", es.paths);
		// Try to find #es.paths with es.path_len (upper bound), by performing DFS from sampled nodes.
		//
		// Get source nodes that have at least one path of length 2.
		log.debug("Sampling %d paths (Alternative approach)", es.paths);
		s.paths = g.V().as("source_node").outE().limit(es.paths/2).inV().outE()
				.select("source_node").by(T.id).limit(es.paths).fold().next().stream()
				.flatMap(source_id -> {
					Vertex v = g.V(source_id).next();
					Long source_uid = forceLong(v.value(ds.uid_field));
					return dfs(v, new HashSet<>(), es.paths_max_len, es.paths, ds.uid_field).stream()
							.peek(p -> {
								p.source_id = source_uid;
								p.source_lbl = v.label();
							});
				})
				.filter(p -> p.sequence.size() >= 2)
				.sorted((a, b) -> b.sequence.size() - a.sequence.size())
				.limit(es.paths)
				.peek(p -> Collections.reverse(p.sequence))
				.collect(Collectors.toList());

		log.debug("Sampling completed");
		return s;
	}

	// NOTE: the "sequence" is built reversed
	private static List<Sample.GPath> dfs(Vertex v, HashSet<Object> visited, int rem_steps, int n_paths, String uid_field) {
		// Mark as visited
		visited.add(v.id());

		List<Sample.GPath> paths = new ArrayList<>();
		if (rem_steps > 0) {
			// Try continue dfs
			v.edges(Direction.OUT).forEachRemaining(e -> {
				Vertex targetVertex = e.inVertex();
				if (visited.contains(targetVertex.id())) {
					return;
				}

				dfs(targetVertex, visited, rem_steps - 1, n_paths, uid_field).stream().peek(p -> {
					// NOTE: "Building in reverse order".
					p.sequence.add(e.label());
				}).collect(Collectors.toCollection(() -> paths));
			});
		}

		if (paths.isEmpty()) {
			Sample.GPath me = new Sample.GPath();
			me.target_id = forceLong(v.value(uid_field));
			me.target_lbl = v.label();
			paths.add(me);
		}

		// top-k on path length
		paths.sort((a, b) -> b.sequence.size() - a.sequence.size());
		return paths.subList(0, Math.min(paths.size(), n_paths));
	}

	public static MappedSamples map(final GraphTraversalSource g, final Dataset ds, final RawSamples s) {
		log.debug("Started sample mapping");
		MappedSamples mapped = new MappedSamples();
		mapped.sample_id = s.sample_id;

		// TODO: improve by mapping all V.uid -> V.real_id in one go.

		log.debug("Mapping nodes");
		mapped.nodes = s.nodes.stream()
				.map(n_uid -> g.V().has(ds.uid_field, n_uid).next().id())
				.collect(Collectors.toList());

		log.debug("Mapping edges");
		mapped.edges = s.edges.stream()
				.map(edge ->
						g.V()
								.has(ds.uid_field, edge.source)
								.outE(edge.label)
								.where(__.inV().has(ds.uid_field, edge.target))
								.next().id()
				).collect(Collectors.toList());

		log.debug("Mapping paths");
		mapped.paths = s.paths.stream()
				.map(p -> {
					MappedSamples.GPath mgp = new MappedSamples.GPath();
					log.debug("Path: %s. Looking for source: %d", p, p.source_id);
					mgp.source_id = g.V().has(ds.uid_field, p.source_id).next().id();
					log.debug("Path: %s. Looking for target: %d", p, p.target_id);
					mgp.target_id = g.V().has(ds.uid_field, p.target_id).next().id();
					return mgp;
				})
				.collect(Collectors.toList());

		log.debug("Sample mapping completed");
		return mapped;
	}

	public static Sample load(String dataset, UUID sample_id) {
		Sample s = new Sample();
		s.raw = mustLoadSample(dataset, sample_id);
		s.mapping = mustLoadMappings(dataset, sample_id);
		s.index();

		if (!(sample_id.equals(s.raw.sample_id) && sample_id.equals(s.mapping.sample_id))) {
			log.fatal("Samples id does not match! DS:%s, Raw:%s, Mapped: %s",
					sample_id, s.raw.sample_id, s.mapping.sample_id);
		}
		s.sample_id = sample_id;

		return s;
	}

	public static RawSamples mustLoadSample(String dataset, UUID sample_id) {
		RawSamples raw = new RawSamples();
		Gson gson = new Gson();

		try {
			raw = gson.fromJson(Files.readString(samplePath(dataset, sample_id)), RawSamples.class);
			assert raw != null;
		} catch (IOException e) {
			e.printStackTrace();
			log.fatal("Error reading raw sample: %s", samplePath(dataset, sample_id));
		}
		return raw;
	}

	public static MappedSamples mustLoadMappings(String dataset, UUID sample_id) {
		Gson gson = new Gson();
		MappedSamples mapping = new MappedSamples();
		try {
			MappedSamplesSerialized m = gson.fromJson(Files.readString(mappingPath(sample_id)), MappedSamplesSerialized.class);
			if (m == null){
				log.fatal("Error parsing mapped file: %s", mappingPath(sample_id));
			}
			assert m != null;

			mapping.sample_id = m.sample_id;

			if (!m.nodes.isEmpty()) {
				mapping.nodes = m.nodes.stream()
						.map(StrSerialize.getDeserializer(m.node_id_class))
						.collect(Collectors.toList());
			}

			if (!m.edges.isEmpty()) {
				mapping.edges = m.edges.stream()
						.map(StrSerialize.getDeserializer(m.edge_id_class))
						.collect(Collectors.toList());
			}

			if (!m.paths_sources.isEmpty()) {
				mapping.paths = new ArrayList<>();
				for (int i=0; i<m.paths_sources.size(); i++)
					mapping.paths.add(new MappedSamples.GPath(m.paths_sources.get(i), m.paths_targets.get(i)));
			}

		} catch (ClassNotFoundException | NoSuchMethodException  e) {
			e.printStackTrace();
			log.fatal("Error casting back ids in mappings");
		} catch (IOException e) {
			e.printStackTrace();
			log.fatal("Error reading sample mapping");
		}
		return mapping;
	}

	public static Path samplePath(String dataset, UUID sample_id) {
		return Paths.get(SAMPLES_DIR, String.format("%s-%s.json", (new File(dataset)).getName(), sample_id));
	}

	// This bill be unique per image.
	public static Path mappingPath(UUID sample_id) {
		return Paths.get(MAPPINGS_DIR, sample_id.toString() + ".json");
	}
}


