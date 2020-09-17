package com.graphbenchmark.queries;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.T;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MixCountInsertSimple extends GenericQuery<Simple.QParam> {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, Simple.QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> res = new ArrayList<>();

		Long cnt;
		long start;


		start = System.currentTimeMillis();
		cnt = gts.V().count().next();
		res.add(new TimingCheckpoint("Clean", System.currentTimeMillis()-start, cnt.toString(), params));

		GdbLogger logger = GdbLogger.getLogger();
		TxContext txCtx = new TxContext(gts);

		// Try to delete up to 3  nodes
		for (int y = 0; y<3; y++) {
			final int x = y;
			GdbLogger.getLogger().ensure(RetryTransactionCtx.retry(20, 0, gts, () -> {
				// Try delete one if possible
				Long tstart = System.currentTimeMillis();
				logger.debug("[T%d] Inserting (local) %d", thread_id, x);

				// NOTE: Must have a label 4 Arango
				gts.getGraph().addVertex(T.label, "person", "uid", Long.valueOf(System.currentTimeMillis()).intValue());

				// NOTE: Following syntax is not available in sqlg
				// gts.addV("person").property("num", System.currentTimeMillis()).iterate(); // Copied from neo4j-tinkerpop-api-impl/issues/19
				res.add(new TimingCheckpoint(String.format("Added (local) %d", x), System.currentTimeMillis() - tstart, params));
				logger.debug("[T%d] Inserted (local) %d", thread_id, x);
				txCtx.commit();
				logger.debug("[T%d] CommittED after (local) %d", thread_id, x);

				tstart = System.currentTimeMillis();
				Long tcnt = gts.V().count().next();
				res.add(new TimingCheckpoint(String.format("[T%d] Count after (local) ~ %d", thread_id, x), System.currentTimeMillis() - tstart, tcnt.toString(), params));
				return null;
			}), "[T%d] abort on inserting node (local) %d due to retry exhaustion.", thread_id, x);
		}

		// Delete some add some
		return res;
	}

	@Override
	public boolean requiresSamples() {
		return true;
	}

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<Simple.QParam>>(){}.getType();
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf c = new QueryConf();
		c.requires_samples = this.requiresSamples();
		c.batch_ok = false;
		c.alt_concurrent_conf = IntStream.range(0, exp.threads)
				.mapToObj(i -> Map.of("invocation", i))
				.collect(Collectors.toList());
		return c;
	}
}

