#META:SID=[0-10]

SID = System.env.get("SID").toInteger();

def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,ID_FIELD_NAME, ID_VALUE,SKIP_COMMIT){

    NUM_INSERTIONS=1000
	v = g.V(id);

	//retrieve node property hashmap
	property_map = v.valueMap().next()

	//------- Node Insertion -----
	t = System.nanoTime();
    for (ins = 0; ins < NUM_INSERTIONS; ins++) {
	query="CREATE (n { ";
	query+=ID_FIELD_NAME;
	query+=": ";
	query+=(ID_VALUE+ins);
	query+=" })";
	graph.cypher(query).iterate();
    	//g.addV().property(ID_FIELD_NAME, ID_VALUE+ins).iterate();

    	if(!SKIP_COMMIT){
    		try {
    			g.tx().commit();
    		} catch (MissingMethodException e) {
    			System.err.println("Does not support g.tx().commit(). Ignoring.");
    		}
	    }
    }
	exec_time = System.nanoTime() - t;
    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE_REPLICATED)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),NUM_INSERTIONS, String.valueOf(OBJECT_ARRAY[i])];
	println result_row.join(',');

}


next_uid = f.infer_type(MAX_UID);

if (SID == NODE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
         execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f.uid_field,next_uid,SKIP_COMMIT);
         order_j++;
         next_uid = next_uid + 1;
	}
} else {
    execute_query(graph,g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f.uid_field,next_uid,SKIP_COMMIT);
}

//g.shutdown();
