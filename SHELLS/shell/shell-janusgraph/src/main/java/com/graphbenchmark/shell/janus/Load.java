package com.graphbenchmark.shell.janus;

import com.google.common.collect.Streams;
import com.graphbenchmark.common.GdbLogger;
import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.schema.v3.Schema;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

@SuppressWarnings("unused")
public class Load extends com.graphbenchmark.queries.mgm.Load {

	@Override
	public Collection<TimingCheckpoint> createLoadIndexUID(GraphTraversalSource gts, Dataset ds, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		imgm.node("__all__", ds.uid_field);
		return List.of(new TimingCheckpoint("Create Index", System.currentTimeMillis()-start, ds.uid_field, (Object)ds.path));
	}

	@Override
	public Collection<TimingCheckpoint> dropLoadIndexUID(GraphTraversalSource gts, Dataset ds, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		imgm.dropNode("__all__", ds.uid_field);
		return List.of(new TimingCheckpoint("Drop Index", System.currentTimeMillis()-start, ds.uid_field, (Object)ds.path));
	}

	@Override
	public Collection<TimingCheckpoint> createSchema(GraphTraversalSource gts, Dataset ds, GenericShell shell) {
		List<TimingCheckpoint> tc = new ArrayList<>();

		long start = System.currentTimeMillis();
		// https://github.com/JanusGraph/janusgraph/blob/cd039d64c64e6d47d3f2e3e8b3c77654b45787d9/janusgraph-examples/example-common/src/main/java/org/janusgraph/example/JanusGraphApp.java#L101
		// Build schema before loading
		Schema.SchemaStream ss = (new Schema(ds.path)).getSchemaStream();
		tc.add(new TimingCheckpoint("Schema", System.currentTimeMillis()-start, "infer", (Object)ds.path));

		start = System.currentTimeMillis();
		JanusGraphManagement m = ((JanusGraph)gts.getGraph()).openManagement();
		try {
			ss.node_labels.sequential().forEach(l -> m.makeVertexLabel(l).make());
			tc.add(new TimingCheckpoint("Schema", System.currentTimeMillis()-start, "-vertex labels & props", (Object)ds.path));

			// m.makePropertyKey(ds.uid_field).dataType(Long.class).cardinality(Cardinality.SINGLE).make(); //Now contained in schema
			// https://docs.janusgraph.org/basics/schema/
			// .. By default, implicitly created edge labels have multiplicity MULTI and implicitly created property keys have cardinality SINGLE and data type Object.class.
			// -> properties name are independent from vertex/edges henche vertex.label1.propA must have the same type as edge.label2.propA
			// -> fallback to Object if multiple defined
			Streams.concat(ss.node_props, ss.edge_props)
					.map(p -> new Pair<>(p.name, p.type))
					.distinct()
					.collect(groupingBy(Pair::getValue0))
					.forEach((name, props) -> {
						Class type = Object.class;
						if (props.size() == 1) {
							try {
								type = Class.forName(props.get(0).getValue1());
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
								GdbLogger.getLogger().fatal("Cannot create property %s with class %s", name, props.get(0).getValue1());
							}
						}

						GdbLogger.getLogger().debug("Creating property %s with class %s. Occurrences %d.", name, type, props.size());
						m.makePropertyKey(name).dataType(type).cardinality(Cardinality.SINGLE).make();
					});

			ss.edge_labels.sequential().filter(l->!m.containsRelationType(l)).forEach(l -> m.makeEdgeLabel(l).make());
			tc.add(new TimingCheckpoint("Schema", System.currentTimeMillis()-start, "-edges labels", (Object)ds.path));

			m.commit(); // This also closes the mgm session
			gts.tx().commit();
			tc.add(new TimingCheckpoint("Schema", System.currentTimeMillis()-start, "-commit", (Object)ds.path));
		} catch (Exception ex) {
			m.rollback();
			ex.printStackTrace();
			GdbLogger.getLogger().fatal("Failed to create schema");
		} finally {
			ss.close();
		}

		return tc;
	}

}
