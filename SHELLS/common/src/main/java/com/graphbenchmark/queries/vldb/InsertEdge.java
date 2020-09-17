package com.graphbenchmark.queries.vldb;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.QSupportSchema;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 *  Q 4 -
 */
public class InsertEdge extends GenericQuery<InsertEdge.QParam> {

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<InsertEdge.QParam>>(){}.getType();
	}

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, InsertEdge.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> ts = new ArrayList<>();
		TxContext tx = new TxContext(gts);

		long start = System.currentTimeMillis();
		RetryTransactionCtx.retryOrDie(gts, thread_id, () -> {
			Vertex sourceNode = gts.V(sample.mapping.nodes.get(params.source)).next(),
				   targetNode = gts.V(sample.mapping.nodes.get(params.target)).next();

			long istart = System.currentTimeMillis();
			sourceNode.addEdge(QSupportSchema.ELabel, targetNode);
			tx.commit();
			ts.add(new TimingCheckpoint("raw operation", System.currentTimeMillis()-istart, params));
			return null;
		});

		ts.add(new TimingCheckpoint(System.currentTimeMillis()-start, String.format("Created: (%s, %s, %s)",
				sample.mapping.nodes.get(params.source), QSupportSchema.ELabel,
				sample.mapping.nodes.get(params.target)), params));
		return ts;
	}

	@Override
	public boolean requiresSamples() {
		return true;
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf c = new QueryConf();
		c.requires_samples = true;
		c.configurations = IntStream.range(0, exp.nodes)
				.mapToObj(i -> Map.of("source", i, "target", (i+1) % exp.nodes))
				.collect(Collectors.toList());
		return c;
	}

	static class QParam extends BaseQParam {
		int source;
		int target;
	}


}

