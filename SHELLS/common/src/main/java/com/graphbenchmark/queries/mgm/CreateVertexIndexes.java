package com.graphbenchmark.queries.mgm;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;
import com.graphbenchmark.common.samples.Property;
import com.graphbenchmark.common.samples.Sample;
import com.graphbenchmark.settings.Dataset;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CreateVertexIndexes extends GenericQuery<CreateVertexIndexesQParam> {

	@Override
	protected Collection<TimingCheckpoint> query(GraphTraversalSource gts, CreateVertexIndexesQParam params, Sample sample, Dataset ds, long thread_id, GenericIndexMgm imgm, GenericShell shell) {
		// If no parameter specified create an index over all sampled properties,
		// otherwise only on the selected sub set.
		List<Property> node_properties = sample.raw.node_props;
		if (params.node_properties != null && !params.node_properties.isEmpty())
			node_properties = params.node_properties;

		// For each label/property: index it.
		return node_properties.stream().map(p -> {
			GdbLogger.getLogger().debug("Indexing %s:%s", p.label, p.name);

			long start = System.currentTimeMillis();
			imgm.node(p.label, p.name);
			return new TimingCheckpoint(
					"Create node-property index",
					System.currentTimeMillis()-start,
					p.label + "::" + p.name,
					params);
		}).collect(Collectors.toList());
	}

	@Override
	public boolean requiresSamples() {
		return true;
	}

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<CreateVertexIndexesQParam>>(){}.getType();
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf q = new QueryConf();
		q.batch_ok = q.concurrent_ok = false;
		q.requires_samples = this.requiresSamples();
		q.common = false;
		// By default, will be invoked once, with no parameters, but with the samples loaded.
		return q;
	}

}

class CreateVertexIndexesQParam extends BaseQParam {
	public List<Property> node_properties;
}
