package com.graphbenchmark.queries.vldb;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Q 38 -
 */
public class ShortestPath extends GenericQuery<ShortestPath.QParam> {
	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<ShortestPath.QParam>>(){}.getType();
	}

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, ShortestPath.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		int length = 0;
		GraphTraversal<Vertex, Path> path = gts.V(sample.mapping.nodes.get(params.source))
				.repeat(__.both().simplePath())
				.until(__.hasId(sample.mapping.nodes.get(params.destination)))
				.path()
				.limit(1);
		if(path.hasNext()){
			Path p = path.next();
			length = p.size();

		}

		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end-start, String.format("Shortest path from %s to %s: length %d",
				sample.raw.nodes.get(params.source), sample.raw.nodes.get(params.destination), length), params));
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
				.mapToObj(source ->  Map.of("source", source, "destination", (source + 1) % exp.nodes))
				.collect(Collectors.toList());
		return c;
	}

	static class QParam extends BaseQParam {
		int source, destination;
	}


}

