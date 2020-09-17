package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllPaths;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Q 40 -
 */
public class NodeKReachabilityOut extends AllPaths {

	final int K = 3;

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		long start_build, end_build, start, end;
		Collection<TimingCheckpoint> ts = new ArrayList<>();
		//Building the traversal

		start_build = System.currentTimeMillis();
		Sample.GPath p = sample.raw.paths.get(params.path);
		Object source = sample.nresovler.r2m(p.source_id);
		Object target = sample.nresovler.r2m(p.target_id);

		GraphTraversal<Vertex, Vertex> _tv = gts.V(source).both();

		int steps = 0;

		for(String _l :  p.sequence.subList(1,K)){
			_tv= _tv.out();
			steps++;
		}
		assert  steps == K;

		_tv = _tv.where(__.hasId(target));

		end_build = System.currentTimeMillis();

		ts.add(new TimingCheckpoint(end_build - start_build, String.format("Build path len  %s",
				K), params));

		// Here is executed
		start = System.currentTimeMillis();
		GraphTraversal<Vertex, Long> _count = _tv.count();
		long numPaths = 0;
		if(_count.hasNext()){
			numPaths = _count.next();
		}
		end = System.currentTimeMillis();
		ts.add(new TimingCheckpoint(end - start, String.format("Found %d outgoing paths from %s to %s : len %s",
				numPaths, p.source_id, p.target_id, K), params));



		return ts;
	}
}
