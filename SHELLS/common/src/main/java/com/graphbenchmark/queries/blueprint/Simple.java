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

public abstract class Simple extends GenericQuery<Simple.QParam> {

	@Override
	public Type getMetaType() {
		return new TypeToken<ArrayList<Simple.QParam>>(){}.getType();
	}

	@Override
	public QueryConf getConf(ExperimentSettings exp) {
		QueryConf c = new QueryConf();
		c.batch_ok = false;
		c.alt_concurrent_conf = IntStream.range(0, exp.threads)
				.mapToObj(i -> Map.of("invocation", i))
				.collect(Collectors.toList());
		return c;
	}

	public static class QParam extends BaseQParam {
		public int invocation;
	}
}


