package com.graphbenchmark.common.schema.v3;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;
import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.samples.Property;
import com.graphbenchmark.common.xgson.ClassTypeAdapter;
import com.graphbenchmark.common.xgson.ClassTypeAdapterFactory;
import org.javatuples.Pair;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.graphbenchmark.settings.Venv.SCHEMAS_DIR;

public class Schema {
	Path ds, fnodes, fedges, fnodeprop, fedgeprop;
	Gson gson;

	public Schema(String dataset) {
		ds = Paths.get(dataset);
		fnodes = Paths.get(SCHEMAS_DIR, ds.toFile().getName() + "_schema_v3_nodes.txt");
		fedges = Paths.get(SCHEMAS_DIR, ds.toFile().getName() + "_schema_v3_edges.txt");
		fnodeprop = Paths.get(SCHEMAS_DIR, ds.toFile().getName() + "_schema_v3_node_prop.txt");
		fedgeprop = Paths.get(SCHEMAS_DIR, ds.toFile().getName() + "_schema_v3_edge_prop.txt");

		// https://stackoverflow.com/questions/29188127/android-attempted-to-serialize-forgot-to-register-a-type-adapter
		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapterFactory(new ClassTypeAdapterFactory());
		gb.registerTypeAdapter(Class.class, new ClassTypeAdapter());
		gson = gb.create();
	}

	public static class SchemaStream {
		public Stream<String> node_labels, edge_labels;
		public Stream<Property> node_props, edge_props;

		public SchemaStream(Stream<String> node_labels, Stream<String> edge_labels,
							Stream<Property> node_props, Stream<Property> edge_props) {
			this.node_labels = node_labels;
			this.edge_labels = edge_labels;
			this.node_props = node_props;
			this.edge_props = edge_props;
		}

		public void close() {
			node_labels.close();
			edge_labels.close();
			node_props.close();
			edge_props.close();
		}
	}

	public SchemaStream getSchemaStream() {
		try {
			if (!fnodes.toFile().exists() || !fedges.toFile().exists() ||
					!fnodeprop.toFile().exists() || !fedgeprop.toFile().exists())
				mineSchema();
		} catch (IOException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Error mining schema for %s", ds);
		}

		try {
			return new SchemaStream(Files.lines(fnodes), Files.lines(fedges),
					Files.lines(fnodeprop).map(Property::fromCsv), Files.lines(fedgeprop).map(Property::fromCsv));
		} catch (IOException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Error reading schema for %s", ds);
		}
		return null;
	}

	public void mineSchema() throws IOException {
		Set<String> nodes = ConcurrentHashMap.newKeySet();
		Set<Property> node_props = ConcurrentHashMap.newKeySet();
		Set<Property> edge_props = ConcurrentHashMap.newKeySet();

		BufferedWriter edgeWriter = new BufferedWriter(new FileWriter(fedges.toFile()));
		Files.lines(ds)
				.parallel()
				.map(l -> gson.fromJson(l, GsonLine.class))
				.filter(l -> l.label != null)
				.peek(l -> nodes.add(l.label))	// Save vertex label
				.peek(l -> node_props.addAll(	// Save vertex properties
						l.properties.entrySet().stream()
								.map(e -> new Property(l.label, e.getKey(), e.getValue().get(0).getType().getName()))
								.collect(Collectors.toUnmodifiableList())))
				.flatMap(l -> Stream.concat((l.outE != null) ? l.outE.entrySet().stream() : Stream.of(),
						(l.inE != null) ? l.inE.entrySet().stream() : Stream.of()))
				.peek(eg -> edge_props.addAll(	// Save edge properties
						eg.getValue().stream()
								.filter(e -> e.properties != null)
								.flatMap(e -> e.properties.entrySet().stream()
										.map(p -> new Property(eg.getKey(), p.getKey(), p.getValue().getType().getName())))
								.collect(Collectors.toUnmodifiableList())))
				.map(Map.Entry::getKey)
				.distinct()
				.forEach(edgeLabel -> {			// Save edge label
					GdbLogger.getLogger().debug(edgeLabel);
					try {
						edgeWriter.write(edgeLabel + System.lineSeparator());
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
			} catch (IOException e) {
				e.printStackTrace();
				GdbLogger.getLogger().fatal("Error saving node labels for %s", ds);
			}
		});
		nodeWriter.close();

		BufferedWriter nodePropWriter = new BufferedWriter(new FileWriter(fnodeprop.toFile()));
		node_props.forEach(prop -> {
			try {
				nodePropWriter.write(prop.toCsv() + System.lineSeparator());
			} catch (IOException e) {
				e.printStackTrace();
				GdbLogger.getLogger().fatal("Error saving node labels for %s", ds);
			}
		});
		nodePropWriter.close();

		BufferedWriter edgePropWriter = new BufferedWriter(new FileWriter(fedgeprop.toFile()));
		edge_props.forEach(prop -> {
			try {
				edgePropWriter.write(prop.toCsv() + System.lineSeparator());
			} catch (IOException e) {
				e.printStackTrace();
				GdbLogger.getLogger().fatal("Error saving node labels for %s", ds);
			}
		});
		edgePropWriter.close();
	}

	public static class GsonLineNoEdges {
		public String label = null;
		public GsonTValue id = null;
		public Map<String, List<GsonProperty>> properties = null;
	}

	public static class GsonLine extends GsonLineNoEdges {
		public Map<String, List<GsonEdgeOut>> outE = null;
		public Map<String, List<GsonEdgeIn>> inE = null;
	}

	public static class GsonBaseEdge {
		public GsonTValue id = null;
		public Map<String, GsonTValue> properties = null;    // No array and no prop_id here
	}

	public static class GsonEdgeIn extends GsonBaseEdge {
		public GsonTValue outV = null;
	}

	public static class GsonEdgeOut extends GsonBaseEdge {
		public GsonTValue inV = null;
	}

	public static class GsonTValue {
		@SerializedName("@type") String type;
		@SerializedName("@value") Object value;

		public Object materialize() {
			return GsonTHelper.materialize(type, value);
		}

		public Class getType() {
			return GsonTHelper.getType(type);
		}

		public GsonTValue specialize() {
			GsonTValue gtv = new GsonTValue();
			gtv.type = this.type;
			gtv.value = this.materialize();
			return GsonTHelper.floatHack(gtv).getValue1();
		}

		@Override
		public String toString() {
			return "GsonTValue{type='" + type + '\'' + ", value=" + value + '}';
		}
	}

	public static class GsonProperty {
		GsonTValue id;
		// Object id;
		// 2 versions:
		// - "value": 12
		// - "value": {"@type": "g:Int32", "@value": 12}
		Object value;

		public Object materialize() {
			if (value instanceof LinkedTreeMap) {
				Object v = ((LinkedTreeMap) value).get("@value");
				String t = (String) ((LinkedTreeMap) value).get("@type");
				return GsonTHelper.materialize(t, v);
			}
			return value;
		}

		public GsonProperty specialize() {
			GsonProperty gp = new GsonProperty();
			gp.id = this.id != null ? this.id.specialize() : null;
			gp.value = this.value;

			if (value instanceof LinkedTreeMap) {
				GsonTValue gtv = new GsonTValue();
				gtv.type = (String) ((LinkedTreeMap) value).get("@type");
				gtv.value = this.materialize();
				Pair<Boolean, GsonTValue> fh = GsonTHelper.floatHack(gtv);
				gp.value = fh.getValue0() ? fh.getValue1() : fh.getValue1().value;
			}

			return gp;
		}

		public Class getType() {
			if (value instanceof LinkedTreeMap) {
				String t = (String) ((LinkedTreeMap) value).get("@type");
				return GsonTHelper.getType(t);
			}
			return value.getClass();
		}

		public Object raw() {
			return value;
		}

		@Override
		public String toString() {
			return "NodePropertyValue{value=" + value + '}';
		}

	}

	public static class GsonTHelper {
		public static Object materialize(String type, Object value) {
			// http://tinkerpop.apache.org/docs/3.4.1/dev/io/
			switch (type) {
				case "g:Date":
					return Date.from(new Timestamp(((Double) value).longValue()).toInstant());
				case "g:Double":
					return Double.valueOf(value.toString());
				case "g:Float":
					return Float.valueOf(value.toString());
				case "g:Int32":
					return ((Double) value).intValue();
				case "g:Int64":
					return ((Double) value).longValue();
				case "g:Timestamp":
					return new Timestamp(((Double) value).longValue());
				case "g:UUID":
					return UUID.fromString((String) value);
				case "g:Class":
					GdbLogger.getLogger().fatal("g:Class properties are not supported");
				case "g:String":
				default:
					return value;
			}
		}

		public static Class getType(String type) {
			// http://tinkerpop.apache.org/docs/3.4.1/dev/io/
			switch (type) {
				case "g:Date":
					return Date.class;
				case "g:Double":
					return Double.class;
				case "g:Float":
					return Float.class;
				case "g:Int32":
					return Integer.class;
				case "g:Int64":
					return Long.class;
				case "g:Timestamp":
					return Timestamp.class;
				case "g:UUID":
					return UUID.class;
				case "g:Class":
					GdbLogger.getLogger().fatal("g:Class properties are not supported");
				case "g:String":
				default:
					return String.class;
			}
		}

		public static Pair<Boolean, GsonTValue> floatHack(GsonTValue tv) {
			boolean safe = true;
			if ((tv.value instanceof Float && (((Float)tv.value).isNaN() || ((Float)tv.value).isInfinite())) ||
					(tv.value instanceof Double && (((Double)tv.value).isNaN() || ((Double)tv.value).isInfinite()))) {
				safe = false;
				tv.value = tv.value.toString();
			}
			return new Pair<>(safe, tv);
		}
	}
}

