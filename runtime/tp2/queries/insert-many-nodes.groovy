#META:SID=[0-10]

SID = System.env.get("SID").toInteger();
NUM_INSERTIONS=1000

def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,id_field_name, id_field_value){

	v = g.v(id);

	//retrieve node property hashmap
	property_map = v.map();
	//change that value

	//------- Node Insertion -----
	t = System.nanoTime();
	for (ins = 0; ins < NUM_INSERTIONS; ins++) {

        property_map[id_field_name] = infer_type(id_field_value) + ins;

	    new_v = g.addVertex(null,property_map);

		if(!SKIP_COMMIT){
			try {
				g.commit();
			} catch (MissingMethodException e) {
				System.err.println("Does not support g.commit(). Ignoring.");
			}
		}
    }
	exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE_REPLICATED)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),NUM_INSERTIONS, String.valueOf(OBJECT_ARRAY[i])];
	println result_row.join(',');

}

if (SID == NODE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
	    execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,uid_field,MAX_UID);
            order_j++;
            MAX_UID = MAX_UID + 1;
	}
} else {
	 execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,uid_field,MAX_UID);
}

//g.shutdown();
