package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllEdges;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;


/**
 * Q 19 -
 */
public class IdSearchEdge extends AllEdges {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllEdges.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		start = System.currentTimeMillis();
		gts.E(sample.mapping.edges.get(params.edge)).next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, params));
	}
}
