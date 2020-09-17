package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.QSupportSchema;
import com.graphbenchmark.queries.blueprint.AllEdges;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Q 21 -
 */
public class UpdateEdgeProperty extends AllEdges {
	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllEdges.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> ts = new ArrayList<>();
		TxContext tx = new TxContext(gts);

		long start = System.currentTimeMillis();
		RetryTransactionCtx.retryOrDie(gts, thread_id, () -> {
			Edge e = gts.E(sample.mapping.edges.get(params.edge)).next();

			long istart = System.currentTimeMillis();
			e.property(QSupportSchema.ECommonProperty, String.format("Hello %s", params.edge));
			tx.commit();
			ts.add(new TimingCheckpoint("raw operation", System.currentTimeMillis()-istart, params));
			return null;
		});
		long end = System.currentTimeMillis();

		ts.add(new TimingCheckpoint(end - start, String.format("Set %s, %s=%s",
				sample.mapping.edges.get(params.edge), QSupportSchema.ECommonProperty,
				String.format("Hello %s", params.edge)), params));
		return ts;
	}
}
