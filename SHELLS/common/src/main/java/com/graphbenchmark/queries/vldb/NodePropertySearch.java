package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.QSupportSchema;
import com.graphbenchmark.queries.blueprint.Simple;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 13 - v2
 */
public class NodePropertySearch extends Simple {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, Simple.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		// Long count = gts.V().has(params.propName, StrSerialize.deserialize(params.propClass, params.propValue)).count().next();
		Long count = gts.V().has(QSupportSchema.VCommonProperty, QSupportSchema.VCommonValue).count().next();
		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end - start, String.format("Found %s nodes with: %s=%s (Should be @samples nodes)",
				count, QSupportSchema.VCommonProperty, QSupportSchema.VCommonValue), params));
	}
}
