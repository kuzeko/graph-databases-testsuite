#META:SID=[0-10]

SID = System.env.get("SID").toInteger();

PROPERTY_NAME= "test_new_specific_property";
PROPERTY_VALUE = "test_value_";

def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID, PROP_NAME, PROP_VAL,SKIP_COMMIT){
	e = g.E(id).next();

	t = System.nanoTime();
	e.property(PROP_NAME, PROP_VAL);

    if(!SKIP_COMMIT){
       try {
          g.tx().commit();
       } catch (MissingMethodException e) {
          System.err.println("Does not support g.tx().commit(). Ignoring.");
       }
    }

	exec_time = System.nanoTime() - t;
        after = e.properties().size();
        //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(EDGE)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),String.valueOf(after),String.valueOf(OBJECT_ARRAY[i].source)+" -- "+OBJECT_ARRAY[i].label+" -> "+ String.valueOf(OBJECT_ARRAY[i].target), String.valueOf(PROP_NAME), String.valueOf(PROP_VAL)];
	println result_row.join(',');
}

if (SID == EDGE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
        execute_query(g,EDGE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID, PROPERTY_NAME, PROPERTY_VALUE,SKIP_COMMIT);
        order_j++;
	}
} else {
    execute_query(g,EDGE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID, PROPERTY_NAME, PROPERTY_VALUE,SKIP_COMMIT);
}

//g.shutdown();
