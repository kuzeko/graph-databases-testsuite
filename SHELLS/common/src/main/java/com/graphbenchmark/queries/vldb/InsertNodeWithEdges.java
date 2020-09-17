package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllNodes;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Q 8 -
 */
public class InsertNodeWithEdges extends AllNodes {
	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllNodes.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> ts = new ArrayList<>();
		TxContext tx = new TxContext(gts);

		long start = System.currentTimeMillis();
		RetryTransactionCtx.retryOrDie(gts, thread_id, () -> {
			Vertex org = gts.V(sample.mapping.nodes.get(params.node)).next();

			long istart = System.currentTimeMillis();
			Vertex n = gts.getGraph().addVertex(T.label, org.label());

			// Copy properties
			org.properties().forEachRemaining(p -> n.property(p.key(), p.value()));
			n.property(ds.uid_field, sample.raw.max_uid + params.node);

			// Copy edges
			org.edges(Direction.OUT).forEachRemaining(e -> n.addEdge(e.label(), e.inVertex()));
			org.edges(Direction.IN).forEachRemaining(e -> e.outVertex().addEdge(e.label(), n));

			tx.commit();
			ts.add(new TimingCheckpoint("raw operation", System.currentTimeMillis()-istart, params));
			return null;
		});

		ts.add(new TimingCheckpoint(System.currentTimeMillis()-start, params));
		return ts;
	}
}
