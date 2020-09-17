package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.List;

/**
 * Q 37 -
 */
public class BFSfiltered extends BFS {
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, BFS.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		long count = gts.V(sample.mapping.nodes.get(params.node))
				.repeat(__.both(sample.raw.edge_labels.toArray(new String[0])).simplePath())
				.until(__.loops().is(params.depth))
				.path()
				.simplePath()
				.count(Scope.global).next();
		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end - start, String.format("Found %d paths from %s at max_depth %s",
				count, sample.mapping.nodes.get(params.node), params.depth), params));
	}
}
