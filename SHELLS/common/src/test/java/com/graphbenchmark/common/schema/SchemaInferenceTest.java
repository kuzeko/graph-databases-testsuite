package com.graphbenchmark.common.schema;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SchemaInferenceTest {

	@Test
	public void testInferProperty() {
		// Possible variation
		// {"age":[{"value": 12}]}
		// {"age":[{"value": {"@type": "g:Int32", "@value": 12}}]}

		ArrayList<String> lines = new ArrayList<>();

		lines.add("{\"properties\":{\"age\":[{\"value\": {\"@type\": \"g:Int32\", \"@value\": 12}}]}}");
		lines.add("{\"properties\":{\"name\":[{\"value\": \"marco\"}]}}");

		// http://tinkerpop.apache.org/docs/3.4.1/dev/io/
		lines.add("{\"properties\":{\"date\":[{\"value\": {\"@type\": \"g:Date\", \"@value\": 1481750076295}}]}}");
		lines.add("{\"properties\":{\"double\":[{\"value\": {\"@type\" : \"g:Double\", \"@value\" : 100.0} }]}}");
		lines.add("{\"properties\":{\"float\":[{\"value\": {\"@type\" : \"g:Float\", \"@value\" : 100.0} }]}}");
		lines.add("{\"properties\":{\"int\":[{\"value\": {\"@type\" : \"g:Int32\", \"@value\" : 100} }]}}");
		lines.add("{\"properties\":{\"long\":[{\"value\": {\"@type\" : \"g:Int64\", \"@value\" : 100} }]}}");
		lines.add("{\"properties\":{\"ts\":[{\"value\": {\"@type\": \"g:Timestamp\", \"@value\": 1481750076295}}]}}");
		lines.add("{\"properties\":{\"uuid\":[{\"value\": { \"@type\" : \"g:UUID\", \"@value\" : \"41d2e28a-20a4-4ab0-b379-d810dede3786\" }}]}}");


		Gson gson = new Gson();
		Map<String, List<List<Object>>> props = lines.stream()
			.map(l -> gson.fromJson(l, GsonLine.class))
			.flatMap(v -> v.properties
					.keySet().stream()
					.map(p -> List.of(p, SchemaInference.inferNodeProperty(v.properties.get(p).get(0)))))
			.collect(Collectors.groupingBy(p -> (String)((List)p).iterator().next()));

		Map<String, Object> res = props.keySet().stream().collect(Collectors.toMap(Function.identity(), k -> {
			Set values = props.get(k).stream().map(v -> {
				Iterator i = v.iterator();
				i.next();
				return i.next();
			}).collect(Collectors.toSet());

			if (values.size() > 1) {
				System.err.printf("Error parsing '%s': %s\n", k, values);
				fail();
			}

			return  values.iterator().next();
		}));

		Map<String, Object> exp = new HashMap<>();
		exp.put("age", 12);
		exp.put("name", "marco");
		exp.put("date", Date.from(new Timestamp(1481750076295L).toInstant()));
		exp.put("double", Double.valueOf("100.0"));
		exp.put("float", Float.valueOf("100.0"));
		exp.put("int", Integer.valueOf("100"));
		exp.put("long", Long.valueOf("100"));
		exp.put("ts", new Timestamp(1481750076295L));
		exp.put("uuid", UUID.fromString("41d2e28a-20a4-4ab0-b379-d810dede3786"));

		assertEquals(exp, res);
	}
}