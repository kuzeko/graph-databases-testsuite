package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllPaths;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Remove : too complex
 */
public class AllPathLabelSearchOut extends AllPaths {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start_build, end_build, start, end;
		Collection<TimingCheckpoint> ts = new ArrayList<>();
		//Building the traversal

		start_build = System.currentTimeMillis();
		Sample.GPath p = sample.raw.paths.get(params.path);

		GraphTraversal<Edge, Vertex> _tv = gts.E(p.sequence.get(0)).outV();
		StringBuilder sb = new StringBuilder();

		for(String _l :  p.sequence.subList(1, p.sequence.size())){
			sb.append(_l).append("->"); // this is just for logging
			_tv= _tv.out(_l);
		}
		end_build = System.currentTimeMillis(); // Up to here the query IS NOT EXECUTED

		String pathString = sb.toString();
		ts.add(new TimingCheckpoint(end_build - start_build, String.format("Build  %s",
				pathString), params));

		// Here is executed
		start = System.currentTimeMillis();
		long numPaths = _tv.count().next();
		end = System.currentTimeMillis();
		ts.add(new TimingCheckpoint(end - start, String.format("Found %d paths from %s : %s",
				numPaths, p.source_id, pathString), params));



		return ts;
	}
}
