package com.graphbenchmark.common;

import com.google.gson.annotations.SerializedName;

public enum ExecutionMode {
    @SerializedName("SINGLE_SHOT")
    SINGLE_SHOT,
    @SerializedName("BATCH")
    BATCH,
    @SerializedName("CONCURRENT")
    CONCURRENT,
}
