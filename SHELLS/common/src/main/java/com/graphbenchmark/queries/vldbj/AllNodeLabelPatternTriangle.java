package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllEdgeLabels;
import com.graphbenchmark.queries.blueprint.AllNodeLabels;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.List;

/**
 * Q 47 -
 */
public class AllNodeLabelPatternTriangle extends AllNodeLabels {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		String node_label = sample.raw.node_labels.get(params.label);
		start = System.currentTimeMillis();
		long edges = gts.V().match(
				__.as("a").hasLabel(node_label)
						.out().as("b"),
				__.as("b").out().as("c"),
				__.as("a").out().as("c")).
				select("a").count().next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, String.format("Found %d triangle with node label %s",
				edges, node_label), params));
	}
}
