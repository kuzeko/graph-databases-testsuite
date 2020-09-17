package com.graphbenchmark.shell.arangodb;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.schema.v3.Schema;
import com.graphbenchmark.common.xgson.ClassTypeAdapter;
import com.graphbenchmark.common.xgson.ClassTypeAdapterFactory;
import com.graphbenchmark.settings.Dataset;
import org.apache.commons.collections4.map.HashedMap;
import org.javatuples.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArangoImpWrapperDisk {
	final Gson gson;
	final Path ds_path;
	final String graph_name;

	final Schema schema;
	final Path workingDir;

	final GdbLogger log = GdbLogger.getLogger();

	final ConcurrentHashMap<Number, String> node_label_map = new ConcurrentHashMap<>();
	final Boolean schemaless = false;

	final String collection_type_doc = "document", collection_type_edge ="edge";

	enum Kind {
		EDGE,
		NODE,
		PROP,
		HAS_PROP
	}
	final long MAX_PROP = 1000; // NOTE: Maximum number of properties per node/edge.

	public ArangoImpWrapperDisk (Dataset ds, String graph_name, Schema s, Path workingDir) {
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapterFactory(new ClassTypeAdapterFactory());
		gb.registerTypeAdapter(Class.class, new ClassTypeAdapter());
		//gb.serializeSpecialFloatingPointValues();
		this.gson = gb.create();

		this.ds_path = Path.of(ds.path);
		this.graph_name = graph_name;
		this.schema = s;
		this.workingDir = workingDir;
	}

	// One file for each collection
	// First pass -> nodes, node props
	// Second pass -> edges, edge props


	public Collection<TimingCheckpoint> exec() throws IOException {
		Collection<TimingCheckpoint> tc = new ArrayList<>();
		long start = System.currentTimeMillis();

		boolean cleanup = true;	// remove the files after loading them

		// First pass
		Pair<Stream<String>, Stream<String>> colls = process_nodes();
		colls.getValue0().forEach(c -> {
			File f = lbl2File(c, true);
			loadCollection(collection_type_doc, c, f);
			if (cleanup) f.delete();
		});
		colls.getValue1().forEach(c -> {
			File f = lbl2File(c, false);
			loadCollection(collection_type_edge, c, f);
			if (cleanup) f.delete();
		});

		tc.add(new TimingCheckpoint("nodes", System.currentTimeMillis()-start, Map.of()));

		// Second pass
		start = System.currentTimeMillis();
		colls = process_edges();
		colls.getValue0().forEach(c -> {
			File f = lbl2File(c, true);
			loadCollection(collection_type_doc, c, f);
			if (cleanup) f.delete();
		});
		colls.getValue1().forEach(c -> {
			File f = lbl2File(c, false);
			loadCollection(collection_type_edge, c, f);
			if (cleanup) f.delete();
		});
		tc.add(new TimingCheckpoint("edges", System.currentTimeMillis()-start, Map.of()));

		return tc;
	}

	Pair<Stream<String>, Stream<String>> process_nodes() throws IOException {

		// Create collection files
		Schema.SchemaStream ss = this.schema.getSchemaStream();
		Map<String, BufferedWriter> collection_files = ss.node_labels.map(lbl -> {
			try {
				return new Pair<>(lbl, new BufferedWriter(new FileWriter(lbl2File(lbl, true))));
			} catch (IOException e) {
				e.printStackTrace();
				log.fatal("Failed creation of %s", Path.of(workingDir.toString(), lbl));
			}
			return null;
		}).collect(Collectors.toUnmodifiableMap(Pair::getValue0, Pair::getValue1));
		BufferedWriter bw_prop =  new BufferedWriter(new FileWriter(lbl2File("ELEMENT-PROPERTIES", true))),
					   bw_has_prop = new BufferedWriter(new FileWriter(lbl2File("ELEMENT-HAS-PROPERTIES", false)));


		Files.lines(this.ds_path)
				.parallel()
				.map(l -> {
					try {
						return gson.fromJson(l, Schema.GsonLineNoEdges.class);
					} catch (Exception ex) {
						log.warning("Error parsing the following line:");
						log.warning(l);
						ex.printStackTrace();
						log.fatal("Import failed parsing dataset");
						return null;
					}
				})
				.forEach(l -> {
					// {"_key":"227","_id":"MAIN_GRAPH_home/227","_rev":"_a4Aj6ZK---","label":"home"},
					// _key					 -> 251
					// _id = collection/id   -> MAIN_GRAPH_ELEMENT-PROPERTIES/251
					HashedMap<String, Object> node = new HashedMap<>();
					Number node_id = (Number)l.id.materialize();

					node.put("_key", node_id.toString());
					node.put("_id", completeID(l.label, node_id, Kind.NODE));
					node.put("label", l.label);

					// Build reverse index for edge creation
					node_label_map.put(node_id, l.label);

					IO.writeLine(collection_files.get(l.label), gson.toJson(node));

					if (l.properties == null)
						return;

					AtomicInteger node_i = new AtomicInteger(0);
					l.properties.entrySet().stream()
							.sorted(Map.Entry.comparingByKey())
							.forEach(pentry -> {
								// --- Property Node
								HashedMap<String, Object> prop = new HashedMap<>();
								Long prop_id = node_id.longValue() * MAX_PROP + node_i.getAndIncrement();
								prop.put("_key", prop_id.toString());
								prop.put("_id", completeID("ELEMENT-PROPERTIES", prop_id, Kind.PROP));
								prop.put("label", "ELEMENT-PROPERTIES");
								prop.put("name", pentry.getKey());
								Schema.GsonProperty p = pentry.getValue().get(0);
								prop.put("value", p.materialize());
								// Yes, really "valutType". Probably an early bug from them.
								prop.put("valutType", p.getType().getName());

								try {
									IO.writeLine(bw_prop, gson.toJson(prop));
								} catch (java.lang.IllegalArgumentException ex) {
									ex.printStackTrace();
									log.fatal("Error encoding property %s for node id %d. Value: %s (%s)", pentry.getKey(), node_id, p.materialize(), p.raw());
								}

								// --- Has-Property Edge
								HashedMap<String, Object> hasprop = new HashedMap<>();
								// We can reuse the property id (collections are namespaces)
								hasprop.put("_key", prop_id.toString());
								hasprop.put("_id", completeID("ELEMENT-HAS-PROPERTIES", prop_id, Kind.HAS_PROP));
								hasprop.put("label", "ELEMENT-HAS-PROPERTIES");
								// From item to property
								hasprop.put("_from", completeID(l.label, node_id, Kind.NODE));
								hasprop.put("_to", completeID("ELEMENT-PROPERTIES", prop_id, Kind.PROP));
								IO.writeLine(bw_has_prop, gson.toJson(hasprop));
							});
				});


		// Close files
		collection_files.values().forEach(bw -> {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				log.fatal("Error closing collection file");
			}
		});
		bw_prop.close();
		bw_has_prop.close();


		return new Pair<>(
				Stream.concat(collection_files.keySet().stream(), Stream.of("ELEMENT-PROPERTIES")),
				Stream.of("ELEMENT-HAS-PROPERTIES"));
	}


	Pair<Stream<String>, Stream<String>> process_edges() throws IOException {

		// Create collection files
		Schema.SchemaStream ss = this.schema.getSchemaStream();
		Map<String, BufferedWriter> collection_files = ss.edge_labels.map(lbl -> {
			try {
				return new Pair<>(lbl, new BufferedWriter(new FileWriter(lbl2File(lbl, false))));
			} catch (IOException e) {
				e.printStackTrace();
				log.fatal("Failed creation of %s", Path.of(workingDir.toString(), lbl));
			}
			return null;
		}).collect(Collectors.toUnmodifiableMap(Pair::getValue0, Pair::getValue1));
		BufferedWriter bw_prop =  new BufferedWriter(new FileWriter(lbl2File("ELEMENT-PROPERTIES", true))),
				bw_has_prop = new BufferedWriter(new FileWriter(lbl2File("ELEMENT-HAS-PROPERTIES", false)));


		Files.lines(this.ds_path)
				.parallel()
				.map(l -> gson.fromJson(l, Schema.GsonLine.class))
				.filter(l -> l.inE != null) // NOTE: Only In edges like tinkerfork.CustomGraphSONReader.java
				.forEach(l -> {
					final String to = completeID(l.label, (Number)l.id.materialize(), Kind.NODE);

					l.inE.entrySet().stream().forEach(emap -> emap.getValue().stream().forEach(e -> {
						HashedMap<String, Object> edge = new HashedMap<>();
						// {"_key":"278","_id":"MAIN_GRAPH_live/278","_from":"MAIN_GRAPH_person/197","_to":"MAIN_GRAPH_home/227","_rev":"_a4Aj6wq---","label":"live"}
						// _key					 -> 251
						// _id = collection/id   -> MAIN_GRAPH_ELEMENT-PROPERTIES/251
						Number edge_id = (Number)e.id.materialize();
						edge.put("_id", completeID(emap.getKey(), edge_id, Kind.EDGE)); // Fuck we need raw node labels
						edge.put("_key", edge_id.toString());
						edge.put("_to", to);
						edge.put("label", emap.getKey());

						Number from = (Number)e.outV.materialize();
						edge.put("_from", completeID(node_label_map.getOrDefault(from, ""), from, Kind.NODE));
						IO.writeLine(collection_files.get(emap.getKey()), gson.toJson(edge));

						if (e.properties == null)
							return;

						AtomicInteger edge_i = new AtomicInteger(0);
						e.properties.entrySet().stream()
							.sorted(Map.Entry.comparingByKey())
							.forEach(pentry -> {
								// --- Property Node
								HashedMap<String, Object> prop = new HashedMap<>();
								Long prop_id = edge_id.longValue() * MAX_PROP + edge_i.getAndIncrement();
								prop.put("_key", prop_id.toString());
								prop.put("_id", completeID("ELEMENT-PROPERTIES", prop_id, Kind.PROP));
								prop.put("label", "ELEMENT-PROPERTIES");
								prop.put("name", pentry.getKey());
								Schema.GsonTValue p = pentry.getValue();
								prop.put("value", p.materialize());
								// Yes, really "valutType". Probably an early bug from them.
								prop.put("valutType", p.getType().getName());
								IO.writeLine(bw_prop, gson.toJson(prop));

								// --- Has-Property Edge
								HashedMap<String, Object> hasprop = new HashedMap<>(); // NOTE: string formatting may be faster.
								hasprop.put("_key", prop_id.toString());
								hasprop.put("_id", completeID("ELEMENT-HAS-PROPERTIES", prop_id, Kind.HAS_PROP));
								hasprop.put("label", "ELEMENT-HAS-PROPERTIES");
								// From item to property
								hasprop.put("_from", completeID(emap.getKey(), edge_id, Kind.EDGE));
								hasprop.put("_to", completeID("ELEMENT-PROPERTIES", prop_id, Kind.PROP));
								IO.writeLine(bw_has_prop, gson.toJson(hasprop));
							});
					}));
				});


		// Close files
		collection_files.values().forEach(bw -> {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				log.fatal("Error closing collection file");
			}
		});
		bw_prop.close();
		bw_has_prop.close();


		return new Pair<>(
				Stream.of("ELEMENT-PROPERTIES"),
				Stream.concat(collection_files.keySet().stream(), Stream.of("ELEMENT-HAS-PROPERTIES")));
	}


	public String completeID(String label, Number id, Kind k) {
		if (this.schemaless) {
			if (k == Kind.NODE)
				return String.format("%s_%c/%d", graph_name, 'V', id);
			if (k == Kind.EDGE)
				return String.format("%s_%c/%d", graph_name, 'E', id);
		}
		return String.format("%s_%s/%d", graph_name, label, id);
	}

	File lbl2File(String lbl, boolean isnode) {
		return Path.of(workingDir.toString(), (isnode ? "V_" : "E_") + lbl).toFile();
	}

	void loadCollection(String type, String collection, File file) {
		// https://lankydan.dev/running-a-java-class-as-a-subprocess
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add("/usr/bin/arangoimp");
		cmd.add("--server.authentication");	cmd.add("false");
		cmd.add("--server.database");		cmd.add("tinkerpop");
		cmd.add("--type");					cmd.add("jsonl");
		cmd.add("--create-database");		cmd.add("true");
		cmd.add("--create-collection");		cmd.add("true");
		cmd.add("--file");					cmd.add(file.getAbsolutePath()); // stdin
		cmd.add("--progress");				cmd.add("false");
		cmd.add("--threads");				cmd.add(Integer.toString(Runtime.getRuntime().availableProcessors() / 2));

		cmd.add("--create-collection-type");	cmd.add(type);
		cmd.add("--collection");				cmd.add(graph_name +"_"+collection);

		// TODO: check if we need any of the following
		// --server.connection-timeout
		// --server.request-timeout

		// This is the default (do not add)
		log.debug("Subprocess with %s", String.join(" ", cmd));

		log.debug("Ready to load %s.%s", type, collection);
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(new File("/"));
		pb.redirectErrorStream(true);	// stderr > &stdout
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT); //  Modify stdout of subprocess (yes, naming is fucked up)

		try {
			Process process = pb.start();
			GdbLogger.getLogger().debug("Waiting for arangoimp to terminate.");
			process.waitFor();
			if (process.exitValue() != 0 )
				GdbLogger.getLogger().fatal("Arangoimp failed on %s with error-code %d.", type, process.exitValue());
		} catch (IOException e) {
			GdbLogger.getLogger().fatal("Error starting arangoimp.");
		} catch (InterruptedException e) {
			GdbLogger.getLogger().fatal("Error while waiting for arangoimp to terminate.");
		}

		GdbLogger.getLogger().debug("%s loaded successfully.", type);
	}

	static class IO {
		final static String lt = "\n";

		public static void writeLine(Writer w, String line) {
			try {
				w.write(line + lt);
			} catch (IOException e) {
				e.printStackTrace();
				GdbLogger.getLogger().fatal("Error writing to file: ", line);
			}
		}
	}


}
