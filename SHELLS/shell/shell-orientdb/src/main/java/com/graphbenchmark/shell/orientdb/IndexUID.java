package com.graphbenchmark.shell.orientdb;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.v3.Schema;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class IndexUID extends GenericQuery<Simple.QParam> {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, Simple.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		OrientGraph og = ((OrientdbShell) shell).mustNotTX(gts);

		final  GdbLogger log = GdbLogger.getLogger();
		log.debug("Indexing UID: %s", ds.uid_field);

		long start = System.currentTimeMillis();
		log.debug("Orient is Label sensitive, for each node label.");

		Set<String> existing_uid = sample.raw.node_props.stream()
				.filter(p -> p.name.equals(ds.uid_field))
				.map(p -> p.label).collect(Collectors.toUnmodifiableSet());

		Schema schema = new Schema(ds.path);
		schema.getSchemaStream().node_labels.forEach(label -> {
			log.debug("Label sensitive, node label: %s.", label);
			if (!existing_uid.contains(label)) {
				log.debug("Create UID property for node label: %s.", label);
				Load.createProperty(og, label, ds.uid_field, Long.class, false);
			}
			imgm.node(label, ds.uid_field);
		});

		return List.of(new TimingCheckpoint("Create uid index[es]",
				System.currentTimeMillis()-start, ds.uid_field, params));
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
		QueryConf q = new QueryConf();
		q.batch_ok = q.concurrent_ok = false;
		q.requires_samples = this.requiresSamples();
		q.common = false;
		return q;
	}
}

