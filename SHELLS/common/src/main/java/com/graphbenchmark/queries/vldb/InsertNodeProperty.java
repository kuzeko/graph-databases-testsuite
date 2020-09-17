package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.QSupportSchema;
import com.graphbenchmark.queries.blueprint.AllNodes;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Q 6 -
 */
public class InsertNodeProperty extends AllNodes {
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllNodes.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> ts = new ArrayList<>();
		TxContext tx = new TxContext(gts);

		long start = System.currentTimeMillis();
		RetryTransactionCtx.retryOrDie(gts, thread_id, () -> {
			Vertex v = gts.V(sample.mapping.nodes.get(params.node)).next();

			long istart = System.currentTimeMillis();
			v.property(QSupportSchema.VProperty, QSupportSchema.VPropertyValue);
			tx.commit();
			ts.add(new TimingCheckpoint("raw operation", System.currentTimeMillis()-istart, params));
			return null;
		});

		ts.add(new TimingCheckpoint(System.currentTimeMillis() - start, String.format("Set %s, %s=%s",
				sample.mapping.nodes.get(params.node), QSupportSchema.VProperty, QSupportSchema.EPropertyValue), params));
		return ts;
	}
}
