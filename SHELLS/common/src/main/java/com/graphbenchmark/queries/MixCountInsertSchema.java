package com.graphbenchmark.queries;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MixCountInsertSchema extends GenericQuery<Simple.QParam> {
	private static AtomicInteger toInsert = new AtomicInteger(0);

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
		for (int x = 0; x<3; x++) {

			// Try delete one if possible
			int dIdx = toInsert.getAndAdd(1);

			GdbLogger.getLogger().ensure(RetryTransactionCtx.retry(20, 5, gts, () -> {
				long tstart = System.currentTimeMillis();
				logger.debug("[T%d] Inserting %d", thread_id, dIdx);

				// Random what:?
				// org.janusgraph.core.SchemaViolationException: Adding this property for key [~T$SchemaName] and value [rtnew_id] violates a uniqueness constraint [SystemIndex#~T$SchemaName]
				// Must have a label 4 Arango
				//gts.addV("person").property("num", 12345).property("new_id", dIdx).iterate();
				logger.debug("[T%d] Before getGraph()", thread_id);
				Graph g = gts.getGraph();

				// This should add 3 vertexes
				logger.debug("[T%d] Before add %d, empty", thread_id, dIdx);
				g.addVertex(T.label, "person");
				// FIXME: Concurrent schema modification ain't thing in postgresql...
				//		  shall we try to add a delay?
				logger.debug("[T%d] Before add %d, w/uid", thread_id, dIdx);
				g.addVertex(T.label, "person", "uid", 1234567);

				logger.debug("[T%d] Before add %d, w/num (new attr)", thread_id, dIdx);
				g.addVertex(T.label, "person", "num", 12345);


				// Are them by def
				// gts.addV().property("num", System.currentTimeMillis()).property("new_id", dIdx);
				res.add(new TimingCheckpoint(String.format("Added %d", dIdx), System.currentTimeMillis() - tstart, params));
				logger.debug("[T%d] Inserted %d", thread_id, dIdx);
				txCtx.commit();
				logger.debug("[T%d] CommittED after %d", thread_id, dIdx);

				tstart = System.currentTimeMillis();
				Long tcnt = gts.V().count().next();
				res.add(new TimingCheckpoint(String.format("[T%d] Count after ~ %d", thread_id, dIdx, params),
						System.currentTimeMillis() - tstart, tcnt.toString()));
				logger.debug("[T%d] Count after %d: %d", thread_id, dIdx, cnt);
				return null;
			}),"[T%d] abort on inserting node %d due to retry exhaustion.", thread_id, dIdx);
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

