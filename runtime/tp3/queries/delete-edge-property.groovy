#META:SID=[0-10]

SID = System.env.get("SID").toInteger(); 

PROPERTY_NAME= "test_common_property";

def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID, PROP_NAME, SKIP_COMMIT){
	e = g.E(id).next();
	t = System.nanoTime();
	c = e.property(PROP_NAME).remove();

        if(!SKIP_COMMIT){
          try {
             g.tx().commit();
          } catch (MissingMethodException e) {
             System.err.println("Does not support g.tx().commit(). Ignoring.");
          }
        }

	exec_time = System.nanoTime() - t;
	//after = g.E(id).properties().next();

        //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(EDGE),PARAMETER2(PROPERTY)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), '', String.valueOf(OBJECT_ARRAY[i].source)+" -- "+OBJECT_ARRAY[i].label+" -> "+ String.valueOf(OBJECT_ARRAY[i].target), String.valueOf(PROP_NAME)];
	println result_row.join(',');
}

if (SID == EDGE_LID_ARRAY.size()) { 
	order_j = 1;
	for (i in RAND_ARRAY) {
       execute_query(g,EDGE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID,PROPERTY_NAME, SKIP_COMMIT);
       order_j++;
	}
} else {
	execute_query(g,EDGE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID,PROPERTY_NAME, SKIP_COMMIT);
}

//g.shutdown();
