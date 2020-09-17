package com.graphbenchmark.shell.janus;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.QSupportSchema;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.schema.JanusGraphManagement;

import java.util.Collection;
import java.util.List;

public class MapSample extends com.graphbenchmark.queries.mgm.MapSample {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, ExperimentSettings params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		// If we use '__all__' it complains that the name 'has already been defined'
		imgm.node("__mapping__", ds.uid_field);
		Collection<TimingCheckpoint> ts = super.query(gts, params, sample, ds, thread_id, imgm, shell);
		imgm.dropNode("__mapping__", ds.uid_field);
		return ts;
	}

	@Override
	public Collection<TimingCheckpoint> qSupportPatch(GraphTraversalSource gts, Sample sample, Dataset ds, GenericShell shell) {
		JanusGraphManagement m = ((JanusGraph)gts.getGraph()).openManagement();

		long start = System.currentTimeMillis();
		try {

			m.makeVertexLabel(QSupportSchema.VLabel).make();
			m.makeEdgeLabel(QSupportSchema.ELabel).make();


			// "new" properties must be known beforehand, otherwise concurrent insertions will fail.
			// org.janusgraph.core.SchemaViolationException: Adding this property for key [~T$SchemaName] and value [rtvertex_test_label_prop_1] violates a uniqueness constraint [SystemIndex#~T$SchemaName]

			PropertyKey vp = m.makePropertyKey(QSupportSchema.VProperty).dataType(QSupportSchema.VPropertyType).cardinality(Cardinality.SINGLE).make();
			PropertyKey vcp = m.makePropertyKey(QSupportSchema.VCommonProperty).dataType(String.class).cardinality(Cardinality.SINGLE).make();
			gts.V(sample.mapping.nodes).label().dedup().toList().forEach(lbl -> {
				// 'New property' support
				m.addProperties(m.getVertexLabel(lbl), vp);
				// 'common property' support
				m.addProperties(m.getVertexLabel(lbl), vcp);
			});

			PropertyKey ep = m.makePropertyKey(QSupportSchema.EProperty).dataType(QSupportSchema.EPropertyType).cardinality(Cardinality.SINGLE).make();
			PropertyKey ecp = m.makePropertyKey(QSupportSchema.ECommonProperty).dataType(String.class).cardinality(Cardinality.SINGLE).make();
			gts.E(sample.mapping.edges).label().dedup().toList().forEach(lbl -> {
				// 'New property' support
				m.addProperties(m.getEdgeLabel(lbl), ep);
				// 'common property' support
				m.addProperties(m.getEdgeLabel(lbl), ecp);
			});

			m.commit();
			return List.of(new TimingCheckpoint("qSupportPatch", System.currentTimeMillis()-start, "-commit", (Object)ds.path));
		} catch (Exception ex) {
			m.rollback();
			ex.printStackTrace();
			GdbLogger.getLogger().fatal("Failed to patch the schema");
		}

		return super.qSupportPatch(gts, sample, ds, shell);
	}
}
