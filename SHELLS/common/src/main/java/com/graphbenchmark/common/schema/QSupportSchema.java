package com.graphbenchmark.common.schema;

public class QSupportSchema {
	public static final String
			VLabel = "vertex_test_label",				// Added to the general schema (no instances)
			ELabel = "edge_test_label",					// Added to the general schema (no instances)
			VProperty = "vertex_test_label_prop_1",		// Added to all sampled nodes' labels schema (no instances)
			EProperty = "edge_test_label_prop_1",		// Added to all sampled edges' labels schema (no instances)

			VCommonProperty = "V_test_common_prop_1",		// Added to all sampled nodes
			VCommonValue = "0x2a",
			ECommonProperty = "edge_test_common_prop_1", 	// Added to all sampled edges
			ECommonValue = "00101010";

	public static final Class
			VPropertyType = Long.class,
			EPropertyType = Long.class;

	public static final Long
			VPropertyValue = 42L,
			EPropertyValue = 52L;
}
