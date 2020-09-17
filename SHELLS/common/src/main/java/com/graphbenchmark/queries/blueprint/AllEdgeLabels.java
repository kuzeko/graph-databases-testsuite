package com.graphbenchmark.queries.blueprint;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.BaseQParam;
import com.graphbenchmark.common.ExperimentSettings;
import com.graphbenchmark.common.GenericQuery;
import com.graphbenchmark.common.QueryConf;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AllEdgeLabels extends GenericQuery<AllEdgeLabels.QParam> {

	public Type getMetaType() {
		return new TypeToken<ArrayList<AllEdgeLabels.QParam>>(){}.getType();
	}

	@Override
	public boolean requiresSamples() {
		return true;
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf c = new QueryConf();
		c.requires_samples = this.requiresSamples();
		c.configurations = IntStream.range(0, exp.edge_labels)
				.mapToObj(i -> Map.of("label", i))
				.collect(Collectors.toList());
		return c;
	}


	public static class QParam extends BaseQParam {
		public int label;
	}

}

