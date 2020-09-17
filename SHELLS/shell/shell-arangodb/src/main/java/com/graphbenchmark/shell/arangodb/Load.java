package com.graphbenchmark.shell.arangodb;

import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.common.schema.v3.Schema;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

public class Load extends com.graphbenchmark.queries.mgm.Load {
	@Override
	public Collection<TimingCheckpoint> query(GraphTraversalSource gts, BaseQParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		try {
			gts.close();
			gts.getGraph().close();
		} catch (Exception e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Error closing the main object graph before arangoimp invoaction");
		}

		try {
			return new ArangoImpWrapperDisk(ds, ((ArangoDbShell)shell).GRAPH_NAME, new Schema(ds.path), Path.of("/")).exec();
		} catch (IOException e) {
			e.printStackTrace();
			GdbLogger.getLogger().fatal("Loading via arangoimp failed.");
		}
		return null;
	}
}
