#META:SID=[0-10]

SID = System.env.get("SID").toInteger();

PROPERTY_NAME= "test_new_specific_property";
PROPERTY_VALUE = "test_value_";

def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,PROP_NAME,PROP_VAL){
	v = g.v(id);

	t = System.nanoTime();
	v.setProperty(PROP_NAME,PROP_VAL);

    if(!SKIP_COMMIT){
      try {
         g.commit();
      } catch (MissingMethodException e) {
         System.err.println("Does not support g.commit(). Ignoring.");
      }
    }
	exec_time = System.nanoTime() - t;
    num = v.map().size()


    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), num, String.valueOf(OBJECT_ARRAY[i]), PROP_NAME, PROP_VAL];
	println result_row.join(',');
}

if (SID == NODE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
	    execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,PROPERTY_NAME,(PROPERTY_VALUE+i));
	    order_j++;
	}
} else {
	 execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,PROPERTY_NAME,(PROPERTY_VALUE+SID));
}

//g.shutdown();
