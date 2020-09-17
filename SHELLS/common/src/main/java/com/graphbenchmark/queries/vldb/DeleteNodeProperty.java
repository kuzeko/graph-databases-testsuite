package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllNodes;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Q 24 -
 */
public class DeleteNodeProperty extends AllNodes {
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllNodes.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> ts = new ArrayList<>();
		TxContext tx = new TxContext(gts);

		long start = System.currentTimeMillis();
		RetryTransactionCtx.retryOrDie(gts, thread_id, () -> {
			Vertex v = gts.V(sample.mapping.nodes.get(params.node)).next();

			long istart = System.currentTimeMillis();
			v.property(ds.uid_field).remove();
			tx.commit();
			ts.add(new TimingCheckpoint("raw operation", System.currentTimeMillis()-istart, params));
			return null;
		});
		ts.add(new TimingCheckpoint(System.currentTimeMillis() - start,
				String.format("Removed %s from %s", ds.uid_field, sample.mapping.nodes.get(params.node)), params));
		return ts;
	}
}
