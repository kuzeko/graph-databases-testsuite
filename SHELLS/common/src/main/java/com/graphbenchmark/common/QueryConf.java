package com.graphbenchmark.common;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;

public class QueryConf {
	// Parameter space already resolved
	public List configurations = ImmutableList.of(new HashMap<>());

	// Alternate configuration for concurrent execution
	// NOTE: If empty list, use `configurations`
	public List alt_concurrent_conf = ImmutableList.of();

	// Can be executed multiple times with the same configuration (multiple iterations)
	public boolean only_once = false;

	// Execution modes
	public boolean single_shot_ok=true, batch_ok=true, concurrent_ok=true;

	// Samples
	public boolean requires_samples=false;

	// Shall be listed in default config
	public boolean common=true;
}
