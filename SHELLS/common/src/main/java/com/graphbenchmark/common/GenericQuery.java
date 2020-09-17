package com.graphbenchmark.common;

import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.lang.reflect.Type;
import java.util.*;

public abstract class GenericQuery<T extends BaseQParam> {
    public GenericQuery() {}

    public boolean requiresSamples() {
        return false;
    }

	public boolean allowsWarmup() {
		return true;
	}

	/**
	 * Implement as follows. T is the `query(params) type`.
	 * {@code
	 *  return new TypeToken<ArrayList<T>>(){}.getType();
	 * }
	 */
	public abstract Type getMetaType();


	public Collection<TimingCheckpoint> execute(GraphTraversalSource gts, T params, Sample sample, Dataset ds, GenericShell shell) {
    	return _execute(gts, params, sample, ds, 0, shell.getIndexManager(gts.getGraph()), shell);
	}

	public Collection<TimingCheckpoint> execute_concurrent(GraphTraversalSource gts, T params, Sample sample, Dataset ds) {
		return _execute(gts, params, sample, ds, Thread.currentThread().getId(), null, null);
	}

	private Collection<TimingCheckpoint> _execute(GraphTraversalSource gts, T params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
    	if (gts == null)
    		GdbLogger.getLogger().fatal("Query invoked without an instantiated Graph object");
		return query(gts, params, sample, ds, thread_id, imgm, shell);
    }

    /**
     * Execute the query on a single configuration of parameters and return the running time.
     */
    protected abstract Collection<TimingCheckpoint> query(GraphTraversalSource gts, T params, Sample sample, Dataset ds, final long thread_id, GenericIndexMgm imgm, GenericShell shell);

	/**
     * A JSON document (wo/new lines), that contains all information to configure the query execution.
     * @return List of json encoded valid configurations, one each line.
     */
    public abstract QueryConf getConf(ExperimentSettings exp);
}

