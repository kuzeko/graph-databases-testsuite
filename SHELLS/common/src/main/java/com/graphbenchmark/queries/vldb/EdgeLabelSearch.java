package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllEdgeLabels;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 16 -
 */
public class EdgeLabelSearch extends AllEdgeLabels {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllEdgeLabels.QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		start = System.currentTimeMillis();
		long edges = gts.E().hasLabel(sample.raw.edge_labels.get(params.label)).count().next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, String.format("Found %d edges with label %s",
				edges, sample.raw.edge_labels.get(params.label)), params));
	}
}
