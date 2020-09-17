package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.List;

/**
 * Remove : too complex
 */
public class AllPatternSquare extends Simple {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		start = System.currentTimeMillis();
		long edges = gts.V().match(
				__.as("a").out().as("b"),
				__.as("b").out().as("c"),
				__.as("a").out().as("d"),
				__.as("d").out().as("c")
				).
				select("a").count().next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, String.format("Found %d triangles",
				edges), params));
	}
}
