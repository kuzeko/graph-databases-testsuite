package com.graphbenchmark.queries.mgm;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.RawSamples;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.samples.SampleManger;
import com.graphbenchmark.common.schema.QSupportSchema;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MapSample extends GenericQuery<ExperimentSettings> {
	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, ExperimentSettings params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> tmc = new ArrayList<>();

		File f = SampleManger.mappingPath(sample.sample_id).toFile();
		if (f.exists())
			GdbLogger.getLogger().fatal("Will not override an existing mapping file.");

		sample.raw = SampleManger.mustLoadSample(ds.path, sample.sample_id);
		sample.mapping = SampleManger.create_mapping_file(gts, sample.raw, ds, sample.sample_id, tmc);
		tmc.addAll(qSupportPatch(gts, sample, ds, shell));
		return tmc;
	}

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<ExperimentSettings>>(){}.getType();
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf qc = new QueryConf();
		qc.configurations = List.of(exp);
		qc.batch_ok = false;
		qc.concurrent_ok = false;
		qc.only_once = true;
		qc.requires_samples = requiresSamples();
		qc.common = false;
		return qc;
	}

	@Override
	public boolean allowsWarmup() {
		return false;
	}


	// NOTE: this code should run each time a new sample is used.
	public Collection<TimingCheckpoint> qSupportPatch(GraphTraversalSource gts, Sample sample, Dataset ds, GenericShell shell) {
		// STEP 4: Implementation may want to get the schema patch from QSupportSchema and modify the schema,
		//         then invoke super on this function.

		// STEP 5: Insert the data.
		Collection<TimingCheckpoint> t = new ArrayList<>();

		long start = System.currentTimeMillis();
		TxContext ctx = new TxContext(gts, false);
		// Set nodes common property
		gts.V(sample.mapping.nodes).property(QSupportSchema.VCommonProperty, QSupportSchema.VCommonValue).iterate();
		// Set edges common property
		gts.E(sample.mapping.edges).property(QSupportSchema.ECommonProperty, QSupportSchema.ECommonValue).iterate();
		ctx.commit();
		t.add(new TimingCheckpoint("inject common values", System.currentTimeMillis()-start, sample.sample_id));

		return t;
	}
}
