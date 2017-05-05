def execute_query(g,id_source,id_destination,i_s,i_d,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID){
	from = g.v(id_source);
	to = g.v(id_destination);
	t = System.nanoTime();
	g.addEdge(from,to,"test_label");
		try {
			g.commit();
		} catch (MissingMethodException e) {
			System.err.println("Does not support g.commit(). Ignoring.");
		}
	exec_time = System.nanoTime() - t;
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time)," ", String.valueOf(OBJECT_ARRAY[i_s]),String.valueOf(OBJECT_ARRAY[i_d])];
	println result_row.join(',');
}
INDEX = System.env.get("INDEX").toInteger();
if (INDEX != NODE_LID_ARRAY.size()) {
	SID = INDEX
	DID = (INDEX + 1) % NODE_LID_ARRAY.size()
	execute_query(g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID);
} else {
	order_j = 1;
	for (i=0; i <RAND_ARRAY.size();i++) {
                SID = i
        DID = (i + 1) % NODE_LID_ARRAY.size()
		execute_query(g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,INDEX);
		order_j++;
	}
}
