package com.graphbenchmark.queries.vldb;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Q 36 -
 */
public class BFS extends GenericQuery<BFS.QParam> {
	final int max_depth = 5;

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<BFS.QParam>>(){}.getType();
	}

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, BFS.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		long count = gts.V(sample.mapping.nodes.get(params.node))
				.repeat(__.both().simplePath())
				.until(__.loops().is(params.depth))
				.path()
				.simplePath()
				.count(Scope.global).next();
		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end-start, String.format("Found %d paths from %s at max_depth %s",
				count, sample.raw.nodes.get(params.node), params.depth), params));
	}

	@Override
	public boolean requiresSamples() {
		return true;
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf c = new QueryConf();
		c.requires_samples = true;
		c.configurations = IntStream.range(0, exp.nodes)
				.boxed()
				.flatMap(node ->
						IntStream.range(2, max_depth).mapToObj(depth ->  Map.of(
							"depth", depth, "node", node)))
				.collect(Collectors.toList());
		return c;
	}

	static class QParam extends BaseQParam {
		int depth, node;
	}


}

