package com.graphbenchmark.settings;

import java.io.File;

public class Venv {
	final public static String MAPPINGS_DIR = "/mappings/";				// [docker] This is supposed to be committed in the image.
	final public static String SAMPLES_DIR = "/runtime/samples/";		// [docker] This can/shall be a volume.
	final public static String SCHEMAS_DIR = "/runtime/schemas/"; 		// [docker] this can/shall be a volume.
	final public static String LOGS_DIR = "/runtime/logs/"; 			// [docker] this can/shall be a volume.

	public static File logFile() {
		return null;
	}
}
