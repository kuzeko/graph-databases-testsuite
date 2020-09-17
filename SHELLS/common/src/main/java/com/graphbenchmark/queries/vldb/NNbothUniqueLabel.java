package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.NN;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 31 -
 */
public class NNbothUniqueLabel extends NN {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, NN.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		long count = gts.V(sample.mapping.nodes.get(params.node)).both().label().dedup().count().next();
		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end - start, String.format("%s got %d",
				sample.mapping.nodes.get(params.node), count), params));
	}
}
