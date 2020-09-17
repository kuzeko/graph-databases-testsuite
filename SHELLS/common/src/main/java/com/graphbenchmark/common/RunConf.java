package com.graphbenchmark.common;

import com.graphbenchmark.settings.Dataset;

import java.util.List;
import java.util.UUID;

public class RunConf {
    public UUID sample_id;
    public Dataset dataset;
    public String query;
    public ExecutionMode mode = ExecutionMode.SINGLE_SHOT;
    public Integer threads = 1;
    public List<String> warmup = List.of(); // Optional argument

    // timeout is in seconds and it is set, by default, at 1 hour.
    public Long timeout = 60 * 60L;

    // Used only for result provenance tracking.
    public String session_id;
    public String cmd_id;
    public String data_suffix="";
}
