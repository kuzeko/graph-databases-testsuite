package com.graphbenchmark.queries.mgm;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.samples.SampleManger;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Sampler extends GenericQuery<ExperimentSettings> {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, ExperimentSettings params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		Collection<TimingCheckpoint> tmc = new ArrayList<>();

		File f = SampleManger.samplePath(ds.path, sample.sample_id).toFile();
		if (f.exists()) {
			GdbLogger.getLogger().fatal("Will not override an existing sample file.");
		}

		SampleManger.create_or_load_sample_file(gts, params, ds, sample.sample_id, tmc);
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
}
