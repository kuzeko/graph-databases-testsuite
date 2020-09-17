package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllPaths;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Q 44 -
 */
public class NodePathLabelSearchOut extends AllPaths {

    final int K = 3;

    @Override
    protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
        long start_build, end_build, start, end;
        Collection<TimingCheckpoint> ts = new ArrayList<>();
        //Building the traversal

        start_build = System.currentTimeMillis();
        Sample.GPath p = sample.raw.paths.get(params.path);
        Object source = sample.nresovler.r2m(p.source_id);


        GraphTraversal<Vertex, Vertex> _tv = gts.V(source);
        StringBuilder sb = new StringBuilder();

        int steps = 0;
        for(String _l :  p.sequence.subList(0, K)){
            sb.append(_l).append("->"); // this is just for logging
            _tv= _tv.out(_l);
            steps++;
        }
        assert  steps == K;

        end_build = System.currentTimeMillis();

        String pathString = sb.toString();
        ts.add(new TimingCheckpoint(end_build - start_build, String.format("Build  %s", pathString), params));

        // Here is executed
        start = System.currentTimeMillis();
        long numPaths = _tv.count().next();
        end = System.currentTimeMillis();
        ts.add(new TimingCheckpoint(end - start, String.format("Found %d OUTGOING paths from %s : %s", numPaths, p.source_id, pathString), params));

        return ts;
    }
}
