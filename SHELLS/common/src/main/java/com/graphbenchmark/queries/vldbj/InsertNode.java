package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllNodes;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Collection;

/**
 *  Q 2 -
 */
public class InsertNode extends AllNodes {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllNodes.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> ts = new ArrayList<>();
		TxContext tx = new TxContext(gts);

		long start = System.currentTimeMillis();
		RetryTransactionCtx.retryOrDie(gts, thread_id, () -> {
			Vertex sourceNode = gts.V(sample.mapping.nodes.get(params.node)).next();

			long istart = System.currentTimeMillis();
			Vertex newVertex = gts.getGraph().addVertex(T.label, sourceNode.label());
//			sourceNode.properties().forEachRemaining(p -> newVertex.property(p.key(), p.value()));
//			newVertex.property(ds.uid_field, sample.raw.max_uid + params.node);
			tx.commit();
			ts.add(new TimingCheckpoint("raw operation", System.currentTimeMillis()-istart, params));
			return null;
		});

		ts.add(new TimingCheckpoint(System.currentTimeMillis() - start,
				String.format("Cloned: %s", sample.mapping.nodes.get(params.node)), params));
		return ts;
	}
}
