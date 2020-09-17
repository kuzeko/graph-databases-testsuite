package com.graphbenchmark.queries.blueprint;

import com.google.gson.reflect.TypeToken;
import com.graphbenchmark.common.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class NN extends GenericQuery<NN.QParam> {

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<NN.QParam>>(){}.getType();
	}

	@Override
	public boolean requiresSamples() {
		return true;
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf c = new QueryConf();
		c.requires_samples = true;
		c.configurations = IntStream.range(0, exp.nodes)
				.mapToObj(i -> Map.of("node", i))
				.collect(Collectors.toList());
		return c;
	}

	public static class QParam extends BaseQParam {
		public int node;
	}

}

