package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllPaths;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.List;

/**
 * Q 48 -
 */
public class NodePatternTriangle extends AllPaths {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		Sample.GPath p = sample.raw.paths.get(params.path);
		Object nodeId = sample.nresovler.r2m(p.source_id);
		start = System.currentTimeMillis();
		long edges = gts.V().match(
				__.as("a").hasId(nodeId).out().as("b"),
				__.as("b").out().as("c"),
				__.as("a").out().as("c")).
				select("c").count().next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, String.format("Found %d triangle with source %s",
				edges, nodeId), params));
	}
}
