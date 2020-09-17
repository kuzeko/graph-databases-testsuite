package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.TxContext;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllPaths;
import com.graphbenchmark.queries.blueprint.NN;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 28 -
 */
public class NNbothFiltered extends AllPaths {
	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllPaths.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Sample.GPath p = sample.raw.paths.get(params.path);
		Object source = sample.nresovler.r2m(p.source_id);
		String _l =  p.sequence.get(0);

		long start = System.currentTimeMillis();
		long count = gts.V(source).bothE(_l).count().next();
		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end - start, String.format("%s has %d edges both direction with sample labels %s",
				source, count, _l), params));
	}
}
