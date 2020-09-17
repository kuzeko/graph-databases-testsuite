package com.graphbenchmark.queries;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.samples.SampleManger;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

/**
 * TestLoader test whether the data has been loaded correctly and if it is indeed accessible.
 * It is expected to be run against: /common/resources/loader_test.json .
 *
 * This query will crash the test suit (i.e. will exit with an error code > 0),
 * when one or more of the tests fail.
 *
 * NOTE: neo4j is expected to fail this test on labels with '::'.
 */
public class TestLoader extends GenericQuery<Simple.QParam> {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, Simple.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> res = new ArrayList<>();
		GdbLogger log = GdbLogger.getLogger();

		boolean suit_nok = false;
		Long cnt;
		boolean ok;

		cnt = gts.V().count().next();
		ok = cnt == 6;
		suit_nok |= !ok;
		log.debug("[ok? %b] #Vertexes: %d, expected: %d", ok, cnt, 6);


		cnt = gts.V().label().dedup().count().next();
		ok = cnt == 4;
		suit_nok |= !ok;
		log.debug("[ok? %b] Vertex labels: %d, expected: %d", ok,cnt, 4);

		cnt = gts.E().count().next();
		ok = cnt == 5;
		suit_nok |= !ok;
		log.debug("[ok? %b] Edges: %d, expected: %d", ok, cnt, 5);

		cnt = gts.E().label().dedup().count().next();
		ok = cnt == 3;
		suit_nok |= !ok;
		log.debug("[ok? %b] Edge labels: %d, expected: %d",ok, cnt, 3);

		// P:2 -pay> P:1 -live> H:3
		Object o = gts.V().has(ds.uid_field, 2).out("pay").out("live").values(ds.uid_field).next();
		ok = SampleManger.forceLong(o) == 3L;
		suit_nok |= !ok;
		log.debug("[ok? %b] P:2 -pay> P:1 -live> H:3 ... found: %s", ok, o);

		// Test not loading outE
		cnt = gts.V().has(ds.uid_field, 1).out("aint").count().next();
		ok |= cnt == 0;
		suit_nok |= !ok;
		log.debug("[ok? %b] Test no load outE, found: %d expected %d",ok, cnt, 0);

		Vertex v2 = gts.V().has(ds.uid_field, 2).next();
		Edge e21 = v2.edges(Direction.OUT, "pay").next();
		o = e21.properties("amount").next().value();
		ok = SampleManger.forceLong(o) == 100L;
		suit_nok |= !ok;
		log.debug("[ok? %b] pay edge should have amount, found: %s expected %d",ok, o, 100);

		// home.4 has a string 'age' property that home.3 doesn not.
		// all people have also an 'age' property but it is an int32!
		Object age = gts.V().has(ds.uid_field, 1).properties("age").next().value();
		ok = age instanceof Integer;
		suit_nok |= !ok;
		log.debug("[ok? %b] Person 'age' should be int32, found: %s (%s)", ok, age.getClass().getName(), age);

		age = gts.V().has(ds.uid_field, 4).properties("age").next().value();
		ok = age instanceof String;
		suit_nok |= !ok;
		log.debug("[ok? %b] House 'age' should be string, found: %s (%s)", ok, age.getClass().getName(), age);

		ok = age.equals("99 years");
		suit_nok |= !ok;
		log.debug("[ok? %b] House 4 'age' found: %s, expected: %s", ok, age, "99 years");

		Boolean age_empty = !gts.V().has(ds.uid_field, 3).properties("age").hasNext();
		suit_nok |= !age_empty;
		log.debug("[ok? %b] House 3 'age' should be empty, is empty? %b", age_empty, age_empty);


		// --- Tests for '::' in labels support
		// https://github.com/neo4j-contrib/neo4j-tinkerpop-api-impl/issues/20
		final String column_lbl = "home::simple";

		// Check if the label is loaded
		String v3_lbl = gts.V().has(ds.uid_field, 3).label().next();
		ok = v3_lbl.equals(column_lbl);
		suit_nok |= !ok;
		log.debug("[ok? %b]  Looking up label for node uid:3 -> %s, expected %s", ok, v3_lbl, "home::simple");

		// Verify hasLabel().count() works
		cnt = gts.V().hasLabel("person").count().next();
		ok = cnt == 2L;
		suit_nok |= !ok;
		log.debug("[ok? %b] Counting vertex with 'person' (testing hasLabel) -> %d, expected %d",ok,  cnt, 2);

		// NOTE: Neo4j is expected to fail here.
		cnt = gts.V().hasLabel(column_lbl).count().next();
		ok = cnt == 2L;
		suit_nok |= !ok;
		log.debug("[ok? %b] Counting vertex with 'home::simple' (ordered) -> %d, expected %d",ok,  cnt, 2);

		// NOTE: Neo4j is expected to fail here.
		v3_lbl = gts.V().has(ds.uid_field, 5).label().next();
		ok = v3_lbl.equals("zzz::aaa");
		suit_nok |= !ok;
		log.debug("[ok? %b] Looking up label for node uid:5 -> %s, expected %s",ok,  v3_lbl, "zzz::aaa");


		// NOTE: Neo4j is expected to fail here.
		cnt = gts.V().hasLabel("zzz::aaa").count().next();
		ok = cnt == 1L;
		suit_nok |= !ok;
		log.debug("[ok? %b] Counting vertex with 'zzz::aaa' (verify no sorting) -> %d, expected %d",ok,  cnt, 1);


		if (suit_nok)
			log.fatal("Loading test-suit failed.");
		log.debug("Loading test-suit passed.");

		return res;
	}

	@Override
	public boolean requiresSamples() {
		return true;
	}

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<Simple.QParam>>(){}.getType();
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf c = new QueryConf();
		c.requires_samples = this.requiresSamples();
		c.batch_ok = false;
		c.concurrent_ok = false;
		return c;
	}
}

