package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllEdgeLabels;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.List;

/**
 * Q 49 -
 */
public class AllEdgeLabelPatternSquare extends AllEdgeLabels {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		String edge_label = sample.raw.edge_labels.get(params.label);
		start = System.currentTimeMillis();
		long edges = gts.V().match(
				__.as("a").out(edge_label).as("b"),
				__.as("b").out().as("c"),
				__.as("a").out().as("d"),
				__.as("d").out().as("c")
				).
				select("c").count().next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, String.format("Found %d square with label %s",
				edges, edge_label), params));
	}
}
