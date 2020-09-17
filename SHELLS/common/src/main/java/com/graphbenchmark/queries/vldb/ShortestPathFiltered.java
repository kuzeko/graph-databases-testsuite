package com.graphbenchmark.queries.vldb;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.Path;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.Collection;
import java.util.List;

/**
 * Q 39 -
 */
public class ShortestPathFiltered extends ShortestPath {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, ShortestPath.QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start = System.currentTimeMillis();
		int length = 0;
		GraphTraversal<Vertex, Path> path = gts.V(sample.mapping.nodes.get(params.source))
				.repeat(__.both(sample.raw.edge_labels.toArray(new String[0])).simplePath())
				.until(__.hasId(sample.mapping.nodes.get(params.destination)))
				.path()
				.limit(1);
		if(path.hasNext()){
			Path p = path.next();
			length = p.size();
		}

		long end = System.currentTimeMillis();

		return List.of(new TimingCheckpoint(end - start, String.format("Shortest path from %s to %s: %d",
				sample.raw.nodes.get(params.source), sample.raw.nodes.get(params.destination), length), params));
	}

}
