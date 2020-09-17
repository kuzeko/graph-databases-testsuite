package com.graphbenchmark.common.schema;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.internal.LinkedTreeMap;
import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.xgson.ClassTypeAdapter;
import com.graphbenchmark.common.xgson.ClassTypeAdapterFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.graphbenchmark.settings.Venv.SCHEMAS_DIR;

public class SchemaInference {

    Path dataset;
    Gson gson;


    public SchemaInference(String dataset) {
        this.dataset = Paths.get(dataset);

        // https://stackoverflow.com/questions/29188127/android-attempted-to-serialize-forgot-to-register-a-type-adapter
        GsonBuilder gb = new GsonBuilder();
        gb.registerTypeAdapterFactory(new ClassTypeAdapterFactory());
        gb.registerTypeAdapter(Class.class, new ClassTypeAdapter());
        gson = gb.create();
    }

    public Schema getSchema() {
        Schema schema = null;
        // Load from file if exists
        File f = this.schemaPath().toFile();
        if (!f.exists()) { // LOAD
            GdbLogger.getLogger().debug("No schema file -> infer.");

            if (f.isDirectory())
                GdbLogger.getLogger().fatal("Schema file is a directory!");

            schema = infer();
            try {
                // Create folder if it does not exists
                File folder = this.schemaPath().getParent().toFile();
                if (!folder.isDirectory() && !folder.mkdirs())
                    throw new IOException();

                FileWriter fw = new FileWriter(f);
                fw.write(gson.toJson(schema));
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
                GdbLogger.getLogger().fatal("Failed to save raw schema file");
            }
        } else {
            GdbLogger.getLogger().debug("Loading schema");

            try {
                schema = gson.fromJson(Files.readString(this.schemaPath()), Schema.class);
                assert schema != null;
            } catch (IOException io) {
                GdbLogger.getLogger().fatal("Error reading schema file: %s", this.schemaPath());
            }
        }

        return schema;
    }


    private Schema infer() {
    	Schema schema = new Schema();
        GdbLogger.getLogger().debug("Inferring schema");
        try {
            Map<Object, String> vertex_label_index = Files.lines(this.dataset)
                    .parallel()
                    .map(l -> gson.fromJson(l, GsonLine.class))
                    .filter(l -> l.label != null)
                    .collect(Collectors.toUnmodifiableMap(l -> l.id.get("@value"), l -> (String)l.label));

            schema.vertex_labels = new TreeSet<>(vertex_label_index.values());

            GdbLogger.getLogger().debug("Got %s vertex labels", vertex_label_index.size());

            Files.lines(this.dataset)
                .parallel()
                .map(l -> gson.fromJson(l, GsonLine.class))
                .filter(l -> l.label != null) // Vertex label
                .forEach(l -> {
                    if (l.outE != null) {
                        for (String edge_label : l.outE.keySet()) {
                            schema.edge_labels.putIfAbsent(edge_label, new HashSet<>()); // TODO: is this thread safe?
                            schema.edge_labels.get(edge_label).addAll(
                                l.outE.get(edge_label).stream()
                                    .map(e -> List.of(l.label, vertex_label_index.get(((Map<String, Object>) e.get("inV")).get("@value"))))
                                    .collect(Collectors.toList())
                            );
                        }
                    }

                    if (l.inE != null) {
                        for (String edge_label : l.inE.keySet()) {
                            schema.edge_labels.putIfAbsent(edge_label, new HashSet<>());
                            schema.edge_labels.get(edge_label).addAll(
                                l.inE.get(edge_label).stream()
                                    .map(e -> List.of(vertex_label_index.get(((Map<String, Object>) e.get("outV")).get("@value")), l.label))
                                    .collect(Collectors.toList())
                            );
                        }
                    }

                    if (l.properties != null) {
                        schema.properties.putIfAbsent(l.label, new HashMap<>());
                        schema.properties.get(l.label).putAll(
                            l.properties.keySet().stream().collect(
                                Collectors.toMap(Function.identity(),
                                p -> SchemaInference.inferNodeProperty(l.properties.get(p).get(0)).getClass())));
                    }
                });
        } catch (IOException e) {
            GdbLogger.getLogger().fatal("Cannot infer schema: dataset file not found");
        }

        GdbLogger.getLogger().debug("Schema inferrence completed");
        return schema;
    }

    static Object inferNodeProperty(NodePropertyValue p) {
        if (p.value instanceof LinkedTreeMap) {
            Object v = ((LinkedTreeMap)p.value).get("@value");
            String t = (String)((LinkedTreeMap) p.value).get("@type");

            // http://tinkerpop.apache.org/docs/3.4.1/dev/io/
            switch (t) {
                case "g:Date":
                    return Date.from(new Timestamp(((Double)v).longValue()).toInstant());
                case "g:Double":
                    return Double.valueOf(v.toString());
                case "g:Float":
                    return Float.valueOf(v.toString());
                case "g:Int32":
                    return ((Double)v).intValue();
                case "g:Int64":
                    return ((Double)v).longValue();
                case "g:Timestamp":
                    return new Timestamp(((Double)v).longValue());
                case "g:UUID":
                    return UUID.fromString((String)v);
                case "g:Class":
                    GdbLogger.getLogger().fatal("g:Class properties are not supported");
                case "g:String":
                    return v.toString();
            }
        }
        return p.value;
    }

    private Path schemaPath() {
        return Paths.get(SCHEMAS_DIR, dataset.toFile().getName() + "_schema.json");
    }
}

class GsonLine {
    String label = null;
    Map<String, Object> id = null;
    Map<String, List<Map<String, Object>>> outE = null;
    Map<String, List<Map<String, Object>>> inE = null;
    Map<String, List<NodePropertyValue>>  properties = null;
}

class NodePropertyValue {
    // Object id;
    // 2 versions:
    // - "value": 12
    // - "value": {"@type": "g:Int32", "@value": 12}
    Object value;

    @Override
    public String toString() {
        return "NodePropertyValue{value=" + value + '}';
    }
}

