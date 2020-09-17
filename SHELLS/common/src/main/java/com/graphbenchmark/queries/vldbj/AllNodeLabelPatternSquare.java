package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllNodeLabels;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.List;

/**
 * Q 50 -
 */
public class AllNodeLabelPatternSquare extends AllNodeLabels {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		String node_label = sample.raw.node_labels.get(params.label);
		start = System.currentTimeMillis();
		long edges = gts.V().match(
				__.as("a").hasLabel(node_label)
						.out().as("b"),
				__.as("b").out().as("c"),
				__.as("a").out().as("d"),
				__.as("d").out().as("c")
		).
				select("c").count().next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, String.format("Found %d square with node label %s",
				edges, node_label), params));
	}
}
