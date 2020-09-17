package com.graphbenchmark.common.schema.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.xgson.ClassTypeAdapter;
import com.graphbenchmark.common.xgson.ClassTypeAdapterFactory;
import org.javatuples.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.graphbenchmark.settings.Venv.SCHEMAS_DIR;

public class Schema {
	Path ds, fnodes, fedges;
	Gson gson;

	public Schema(String dataset) {
		ds = Paths.get(dataset);
		fnodes = Paths.get(SCHEMAS_DIR, ds.toFile().getName() + "_schema_v2_nodes.txt");
		fedges = Paths.get(SCHEMAS_DIR, ds.toFile().getName() + "_schema_v2_edges.txt");

		// https://stackoverflow.com/questions/29188127/android-attempted-to-serialize-forgot-to-register-a-type-adapter
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapterFactory(new ClassTypeAdapterFactory());
		gb.registerTypeAdapter(Class.class, new ClassTypeAdapter());
		gson = gb.create();
	}


	public Pair<Stream<String>, Stream<String>> getLabelStreams() {
		try {
			if (!fnodes.toFile().exists() || !fedges.toFile().exists())
				mineSchema();
		} catch (IOException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Error mining schema for %s", ds);
		}

		try {
			return new Pair<>(Files.lines(fnodes), Files.lines(fedges));
		} catch (IOException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Error reading schema for %s", ds);
		}
		return null;
	}

	public void mineSchema() throws IOException {
		Set<String> nodes = ConcurrentHashMap.newKeySet();

		BufferedWriter edgeWriter = new BufferedWriter(new FileWriter(fedges.toFile()));
		Files.lines(ds)
				.parallel()
				.map(l -> gson.fromJson(l, GsonLine.class))
				.filter(l -> l.label != null) 	// Vertex label
				.peek(l -> nodes.add(l.label))	// Save vertex label
				.flatMap(l -> Stream.concat((l.outE != null) ? l.outE.keySet().stream() : Stream.of(),
						(l.inE != null) ? l.inE.keySet().stream() : Stream.of()))
				.distinct()
				.forEach(edgeLabel -> {
					try {
						edgeWriter.write(edgeLabel + System.lineSeparator());
						//edgeWriter.newLine();
					} catch (IOException e) {
						e.printStackTrace();
						GdbLogger.getLogger().fatal("Error saving edge labels for %s", ds);
					}
				});
		edgeWriter.close();

		BufferedWriter nodeWriter = new BufferedWriter(new FileWriter(fnodes.toFile()));
		nodes.forEach(nodeLabel -> {
			try {
				nodeWriter.write(nodeLabel + System.lineSeparator());
				//nodeWriter.newLine();
			} catch (IOException e) {
				e.printStackTrace();
				GdbLogger.getLogger().fatal("Error saving node labels for %s", ds);
			}
		});
		nodeWriter.close();
	}
}

class GsonLine {
	String label = null;
	Map<String, Object> outE = null;
	Map<String, Object> inE = null;
}