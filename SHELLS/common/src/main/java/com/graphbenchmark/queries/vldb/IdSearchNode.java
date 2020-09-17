package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllNodes;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 18 -
 */
public class IdSearchNode extends AllNodes {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllNodes.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		start = System.currentTimeMillis();
		gts.V(sample.mapping.nodes.get(params.node)).next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, params));
	}
}
