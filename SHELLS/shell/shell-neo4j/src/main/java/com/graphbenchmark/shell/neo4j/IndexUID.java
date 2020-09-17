package com.graphbenchmark.shell.neo4j;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;

public class IndexUID extends com.graphbenchmark.queries.mgm.IndexUID {
	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, Simple.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		IndexMgm neo_imgm = (IndexMgm)imgm;
		neo_imgm.waitForIndex = false;
		Collection<TimingCheckpoint> ts = super.query(gts, params, sample, ds, thread_id, neo_imgm, shell);
		neo_imgm.waitForIndex();
		return ts;
	}
}
