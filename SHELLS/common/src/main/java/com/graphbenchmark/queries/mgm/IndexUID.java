package com.graphbenchmark.queries.mgm;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.v3.Schema;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IndexUID extends GenericQuery<Simple.QParam> {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, Simple.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		final  GdbLogger log = GdbLogger.getLogger();
		log.debug("Indexing UID: %s", ds.uid_field);

		long start = System.currentTimeMillis();
		if (!imgm.labelSensitive()) {
			log.debug("Not label sensitive.");
			imgm.node("__all_labels__", ds.uid_field);
	 	} else {
			log.debug("Label sensitive, for each node label.");

			Schema schema = new Schema(ds.path);
			schema.getSchemaStream().node_labels.forEach(label -> {
				log.debug("Label sensitive, node label: %s.", label);
				imgm.node(label, ds.uid_field);
			});
		};

		return List.of(new TimingCheckpoint("Create uid index[es]",
				System.currentTimeMillis()-start, ds.uid_field, params));
	}

	@Override
	public boolean requiresSamples() {
		return false;
	}

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<Simple.QParam>>(){}.getType();
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf q = new QueryConf();
		q.batch_ok = q.concurrent_ok = false;
		q.requires_samples = this.requiresSamples();
		q.common = false;
		return q;
	}
}

