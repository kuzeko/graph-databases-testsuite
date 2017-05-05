SID = System.env.get("SID").toInteger(); 
PROPERTY_NAME= "test_specific_property";
PROPERTY_VALUE = "test_value_";
def execute_query(g,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,SID,PROP_NAME,PROP_VAL){
	t = System.nanoTime();
	count = g.V.has(PROP_NAME,PROP_VAL).count().next();
	exec_time = System.nanoTime() - t;
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), count, String.valueOf(PROP_NAME), String.valueOf(PROP_VAL)];
	println result_row.join(',');
}
if (SID == NODE_ARRAY.size()) { 
	order_j = 1;
	for (i in RAND_ARRAY) {
	    execute_query(g,order_j,DATABASE,DATASET,QUERY,ITERATION,SID,PROPERTY_NAME,(PROPERTY_VALUE+i));
	    order_j++;
	}
} else {
	 execute_query(g,0,DATABASE,DATASET,QUERY,ITERATION,SID,PROPERTY_NAME,(PROPERTY_VALUE+(SID+1)));
}
