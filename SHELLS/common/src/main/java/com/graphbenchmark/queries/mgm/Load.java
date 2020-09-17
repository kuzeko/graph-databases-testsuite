package com.graphbenchmark.queries.mgm;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.queries.mgm.tinkerfork.CustomGraphSONReader;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Load extends GenericQuery<BaseQParam> {

    @Override
    public Type getMetaType() {
        return new TypeToken<ArrayList<BaseQParam>>(){}.getType();
    }

    @Override
    public Collection<TimingCheckpoint> query(GraphTraversalSource gts, BaseQParam params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell) {
        Collection<TimingCheckpoint> t = new ArrayList<>();
        long start, end;

        try {
            // STEP 1: Create schema (if required)
            t.addAll(createSchema(gts, ds, shell));

            // STEP 2.0:
            t.addAll(createLoadIndexUID(gts, ds, imgm, shell));

            // STEP 2.1: Load dataset (via standard API)
            TxContext ctx = new TxContext(gts, false);
            start = System.currentTimeMillis();
            //GraphSONReader.build()
            // TODO: consider creating an index on UID before loading and then removing it (especially for janus)
            CustomGraphSONReader.build()
                    .mapper(GraphSONMapper.build().version(GraphSONVersion.V3_0).create())
                    .create().readGraph(new File(ds.path), gts.getGraph(), ds.uid_field);
            end = System.currentTimeMillis();
            ctx.commit();
            t.add(new TimingCheckpoint("readGraph", end-start, params));

            // STEP 2.2:
            t.addAll(dropLoadIndexUID(gts, ds, imgm, shell));

            // Steps 3-5 moved to Sample and MapSample.
            return t;
        } catch (IOException e) {
            e.printStackTrace();
            GdbLogger.getLogger().fatal("Error importing the graph");
        }

        return null;
    }

    public Collection<TimingCheckpoint> createLoadIndexUID(GraphTraversalSource gts, Dataset ds, GenericIndexMgm imgm, GenericShell shell) {
    	GdbLogger.getLogger().debug("Loading: not creating an index for UID");
    	return List.of();
	}

    public Collection<TimingCheckpoint> dropLoadIndexUID(GraphTraversalSource gts, Dataset ds, GenericIndexMgm imgm, GenericShell shell) {
        GdbLogger.getLogger().debug("Loading: no index to drop for UID");
        return List.of();
    }

    public Collection<TimingCheckpoint> createSchema(GraphTraversalSource gts, Dataset ds, GenericShell shell) {
    	// Implementation may want to get the schema from Schema.getSchema() and materialize it.
        return List.of();
    }

    @Override
    public boolean allowsWarmup() {
        return false;
    }

    @Override
    public QueryConf getConf(ExperimentSettings exp) {
        QueryConf qc = new QueryConf();
        qc.only_once = true;
        qc.batch_ok = false;
        qc.concurrent_ok = false;
        qc.requires_samples = requiresSamples();
        qc.configurations = List.of(exp);
        qc.common = false;
        return qc;
    }
}

