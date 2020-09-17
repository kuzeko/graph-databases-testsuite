package com.graphbenchmark.utils;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.graphbenchmark.common.schema.v3.Schema;
import com.graphbenchmark.common.xgson.ClassTypeAdapter;
import com.graphbenchmark.common.xgson.ClassTypeAdapterFactory;
import org.javatuples.Pair;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Given a dataset encoded in GraphSON 3 it sanitizes labels and property names.
 *
 *
 * Note: floats/double NaN and Infinity are encoded with the correct type but then the values is wrapped in a string.
 *       This is due to the JSON standard not supporting these values.
 */

public class Sanitizer {
	enum HPrefix {
		NODE_PROP("NP_"),
		EDGE_PROP("EP_"),
		NODE_LABEL("NLBL_"),
		EDGE_LABEL("ELBL_");

		public final String prefix;

		HPrefix(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public String toString() {
			return prefix;
		}
	};

	public Gson bootstrapGson() {
		// https://stackoverflow.com/questions/29188127/android-attempted-to-serialize-forgot-to-register-a-type-adapter
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapterFactory(new ClassTypeAdapterFactory());
		gb.registerTypeAdapter(Class.class, new ClassTypeAdapter());
		return gb.create();
	}

	String hash(String txt, HPrefix prefix) {
		// must be < 64, better if < 32
		// e.g. https://www.postgresql.org/docs/12/limits.html
		switch (txt) {
			// Whitelisted property names
			case "id":
			case "uid":
				return txt;
		}
		return prefix + Hashing.murmur3_128().hashString(txt, StandardCharsets.UTF_8).toString();
	}

	public void sanitize(Path ds) throws IOException {
		Gson gson = bootstrapGson();
		Files.lines(ds)
				.parallel()
				.map(l -> gson.fromJson(l, Schema.GsonLine.class))
				.map(l -> {
					Schema.GsonLine new_line = new Schema.GsonLine();
					new_line.id = l.id.specialize();
					new_line.label = hash(l.label, HPrefix.NODE_LABEL);

					new_line.properties = Map.of();
					if (l.properties != null)
						new_line.properties = l.properties.entrySet().stream()
								.collect(Collectors.toUnmodifiableMap(
										e -> hash(e.getKey(), HPrefix.NODE_PROP),
										me -> me.getValue().stream().map(Schema.GsonProperty::specialize).collect(Collectors.toUnmodifiableList())));

					new_line.inE = Map.of();
					if (l.inE != null)
						new_line.inE = l.inE.entrySet().stream().map(edge_entry -> new Pair<>(
								hash(edge_entry.getKey(), HPrefix.EDGE_LABEL),
								edge_entry.getValue().stream().map(e -> {
									Schema.GsonEdgeIn new_ein = new Schema.GsonEdgeIn();
									new_ein.id = e.id.specialize();
									new_ein.outV = e.outV;
									new_ein.properties = Map.of();
									if (e.properties != null)
										new_ein.properties = e.properties.entrySet().stream()
												.collect(Collectors.toUnmodifiableMap(
														ep -> hash(ep.getKey(), HPrefix.EDGE_PROP),
														me -> me.getValue().specialize()));

									return new_ein;
								}).collect(Collectors.toUnmodifiableList()))
						).collect(Collectors.toUnmodifiableMap(Pair::getValue0, Pair::getValue1));

					return new_line;
				})
				.map(gson::toJson)
				.forEach(System.out::println);
	}


	public static void main(String ... args) {
		if (args.length != 2) {
			System.out.printf("Usage %s source_dataset.jsonl\n", args[0]);
			System.exit(1);
		}

		Sanitizer s = new Sanitizer();
		try {
			s.sanitize(Path.of(args[1]));
		} catch (IOException e) {
			System.out.printf("Error opening the file %s.\n", args[1]);
			System.exit(1);
		}
	}
}
