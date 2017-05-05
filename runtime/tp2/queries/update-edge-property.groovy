SID = System.env.get("SID").toInteger(); 
PROPERTY_NAME= "test_common_property";
PROPERTY_VALUE = "test_value";
def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,PROP_NAME,PROP_VAL){
	e = g.e(id);
	t = System.nanoTime();
	e.setProperty(PROP_NAME, PROP_VAL);
        if(!SKIP_COMMIT){
          try {
             g.commit();
          } catch (MissingMethodException e) {
             System.err.println("Does not support g.commit(). Ignoring.");
          }
        }
	exec_time = System.nanoTime() - t;
        props = e.map()
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),String.valueOf(props), String.valueOf(OBJECT_ARRAY[i].source)+" -- "+OBJECT_ARRAY[i].label+" -> "+ String.valueOf(OBJECT_ARRAY[i].target), String.valueOf(PROP_NAME), String.valueOf(PROP_VAL)];
	println result_row.join(',');
}
if (SID == EDGE_LID_ARRAY.size()) { 
	order_j = 1;
	for (i in RAND_ARRAY) {
        execute_query(g,EDGE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID,PROPERTY_NAME,(PROPERTY_VALUE+"NEW"));
        order_j++;
	}
} else {
    execute_query(g,EDGE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID,PROPERTY_NAME,(PROPERTY_VALUE+"NEW"));
}
