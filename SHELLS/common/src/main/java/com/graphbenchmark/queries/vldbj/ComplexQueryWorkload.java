package com.graphbenchmark.queries.vldbj;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.blueprint.AllEdges;
import com.graphbenchmark.queries.blueprint.AllNodes;
import com.graphbenchmark.queries.vldb.IdSearchEdge;
import com.graphbenchmark.queries.vldb.IdSearchNode;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.util.ArrayList;
import java.util.Collection;


/**
 * NOT READY
 */
public class ComplexQueryWorkload extends AllNodes {


    @Override
    protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, QParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {

        Collection<TimingCheckpoint> checkpoints = new ArrayList<>();

        IdSearchNode q1 = new IdSearchNode();


        // Verify that params are of the correct type!
        checkpoints.addAll(q1.execute_concurrent(gts, params, sample, ds)); // -->ThreadID is always set also in single mode

        AllEdges.QParam eParams = new AllEdges.QParam();
        // Set the params.edge property

        eParams.edge = params.node%sample.raw.edges.size(); // ensure id is in the correct size

        IdSearchEdge ids;


        return checkpoints;
    }
}