package com.graphbenchmark.queries;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MixCountDelete extends GenericQuery<Simple.QParam> {
	private static AtomicInteger toDelete = new AtomicInteger(0);

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, Simple.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> res = new ArrayList<>();

		Long cnt;
		long start;


		start = System.currentTimeMillis();
		cnt = gts.V().count().next();
		res.add(new TimingCheckpoint("Clean", System.currentTimeMillis()-start, cnt.toString(), params));

		GdbLogger logger = GdbLogger.getLogger();
		TxContext txCtx = new TxContext(gts);

		// Try delete one if possible
		int dIdx = toDelete.getAndIncrement();
		if (dIdx >= sample.mapping.nodes.size())
			return res;
		logger.debug("[T%d] Trying to delete %d [uid %s] -> %s", thread_id,
				dIdx, sample.raw.nodes.get(dIdx), sample.mapping.nodes.get(dIdx));
		Object v = sample.mapping.nodes.get(dIdx);

		GdbLogger.getLogger().ensure(
			RetryTransactionCtx.retry(20, 0, gts, () -> {
				long tstart = System.currentTimeMillis();
				gts.V(v).drop().iterate();
				res.add(new TimingCheckpoint(String.format("[T%d] Removed %d", thread_id, dIdx, params),
						System.currentTimeMillis() - tstart, v.toString()));
				logger.debug("[T%d] Removed to delete %d [uid %s] -> %s", thread_id,
						dIdx, sample.raw.nodes.get(dIdx), sample.mapping.nodes.get(dIdx));
				logger.debug("[T%d] Committing after %d [uid %s] -> %s", thread_id,
						dIdx, sample.raw.nodes.get(dIdx), sample.mapping.nodes.get(dIdx));
				txCtx.commit();
				logger.debug("[T%d] CommittED after %d [uid %s] -> %s", thread_id,
						dIdx, sample.raw.nodes.get(dIdx), sample.mapping.nodes.get(dIdx));
				return null;
			}), "[T%d] Abort node deletion due to try exhaust", thread_id);

		start = System.currentTimeMillis();
		cnt = gts.V().count().next();
		res.add(new TimingCheckpoint(String.format("Count after ~ %d", dIdx), System.currentTimeMillis() - start, cnt.toString(), params));

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

