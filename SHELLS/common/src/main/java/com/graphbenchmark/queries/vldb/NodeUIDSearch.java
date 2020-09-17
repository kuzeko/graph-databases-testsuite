package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllNodes;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 13 -
 */
public class NodeUIDSearch extends AllNodes {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllNodes.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		String value  = gts.V().has(ds.uid_field, sample.raw.nodes.get(params.node)).properties(ds.uid_field).value().next().toString();
		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end - start, String.format("Found node with: %s=%s=%s",
				ds.uid_field, sample.raw.nodes.get(params.node),value), params));
	}


}
