package com.graphbenchmark.common;


import com.google.gson.Gson;

public class TimingCheckpoint {
	private static Gson gson = new Gson();
    private String checkpoint = "--";  // TODO: find a better default
    private Long time = -1l;
    private String details = "--";
    private Object params;

    public TimingCheckpoint(Long time, Object params) {
        this.time = time;
        this.params = params;
    }

    public TimingCheckpoint(String checkpoint, Long time, Object params) {
        this.checkpoint = checkpoint;
        this.time = time;
        this.params = params;
    }

    public TimingCheckpoint(String checkpoint, Long time, String details, Object params) {
        this.checkpoint = checkpoint;
        this.time = time;
        this.details = details;
        this.params = params;
    }

    public TimingCheckpoint(Long time, String details, Object params) {
        this.time = time;
        this.details = details;
        this.params = params;
    }

    @Override
    public String toString() {
        return String.format("%s;%sms;%s;%s",
                             checkpoint,
                             time.toString(),
                             details.replace(';', '_'),
                             params instanceof String ? params : gson.toJson(params));
    }
}
