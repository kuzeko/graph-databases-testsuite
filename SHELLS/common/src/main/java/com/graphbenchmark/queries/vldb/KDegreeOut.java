package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

import java.util.Collection;
import java.util.List;

/**
 * Q 33 -
 */
public class KDegreeOut extends Simple {

	final long K = 50;

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, Simple.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start, end;
		start = System.currentTimeMillis();
		long count = gts.V().where(__.outE().count().is(P.gte(K))).count().next();
		end = System.currentTimeMillis();
		return List.of(new TimingCheckpoint(end - start, String.format("nodes with out-k > %d: %d", K, count), params));
	}

}
