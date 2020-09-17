package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.TxContext;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.NN;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 27 -
 */
public class NNoutgoing extends NN {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, NN.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		long count = gts.V(sample.mapping.nodes.get(params.node)).outE().count().next();
		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end - start, String.format("%s has %d outgoing edges",
				sample.mapping.nodes.get(params.node), count), params));
	}
}
