package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.GenericIndexMgm;
import com.graphbenchmark.common.GenericShell;
import com.graphbenchmark.common.TimingCheckpoint;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllNodeLabels;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.Collection;
import java.util.List;

/**
 * Q 17 - *
 */
public class NodeLabelSearch extends AllNodeLabels {


    @Override
    protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, AllNodeLabels.QParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
        long start, end;
        String node_label = sample.raw.node_labels.get(params.label);
        start = System.currentTimeMillis();
        long edges = gts.V().hasLabel(node_label).count().next();
        end = System.currentTimeMillis();
        return List.of(new TimingCheckpoint(end - start, String.format("Found %d nodes with label %s",
                edges, node_label), params));
    }

}
