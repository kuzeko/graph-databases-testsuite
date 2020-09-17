package com.graphbenchmark.shell.sqlgpg;

import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.QSupportSchema;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.javatuples.Pair;

import java.util.Collection;
import java.util.HashSet;

@SuppressWarnings("unused")
public class MapSample extends com.graphbenchmark.queries.mgm.MapSample {
	// TODO: WATCH OUT! what happen if we have the same edge label between two different nodes labels?

	// We shall create the schema part for concurrent queries (otherwise will fail)
	@Override
	public Collection<TimingCheckpoint> qSupportPatch(GraphTraversalSource gts, Sample sample, Dataset ds, GenericShell shell) {


		// Create vertex label
		Vertex n = gts.getGraph().addVertex(QSupportSchema.VLabel);
		n.property(QSupportSchema.VProperty, QSupportSchema.VPropertyValue);
		gts.tx().commit();
		n.remove();
		gts.tx().commit();

		HashSet<String> node_labels = new HashSet<>();
		HashSet<Pair<String, String>> edges = new HashSet<>();
		for (int i=0; i<sample.mapping.nodes.size(); i++) {
			Vertex sourceNode = gts.V(sample.mapping.nodes.get(i)).next(),
					targetNode = gts.V(sample.mapping.nodes.get((i+1)%sample.mapping.nodes.size())).next();
			if (!node_labels.contains(sourceNode.label())) {
				// Create property for this label
				node_labels.add(sourceNode.label());
				sourceNode.property(QSupportSchema.VProperty, QSupportSchema.VPropertyValue);
				gts.tx().commit();
				sourceNode.property(QSupportSchema.VProperty).remove();
				gts.tx().commit();
			}

			Pair<String, String> edge = new Pair<>(sourceNode.label(), targetNode.label());
			if (!edges.contains(edge)) {
				// Create edge with properties
				edges.add(edge);
				sourceNode.addEdge(QSupportSchema.ELabel, targetNode, QSupportSchema.EProperty, QSupportSchema.EPropertyValue);
				gts.tx().commit();
				sourceNode.edges(Direction.OUT, QSupportSchema.ELabel).forEachRemaining(Element::remove);
				gts.tx().commit();
			}
		}


		HashSet<String> edge_labels = new HashSet<>();
		gts.E(sample.mapping.edges).toList().forEach(e -> {
			if (edge_labels.contains(e.label()))
				return;
			edge_labels.add(e.label());
			Property<Long> p = e.property(QSupportSchema.EProperty, QSupportSchema.EPropertyValue);
			p.remove();
			gts.tx().commit();
		});

		return super.qSupportPatch(gts, sample, ds, shell);
	}
}
