package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllNodes;
import com.graphbenchmark.queries.blueprint.AllPaths;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 14 -
 */
public class NodeLabelledPropertySearch extends AllPaths {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {

		Sample.GPath p = sample.raw.paths.get(params.path);

		long start = System.currentTimeMillis();
		String value  = gts.V().has(p.source_lbl, ds.uid_field, p.source_id).properties(ds.uid_field).value().next().toString();
		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end - start, String.format("Found node with label %s and %s=%s=%s",
				p.source_lbl, ds.uid_field, p.source_id,value), params));
	}


}
