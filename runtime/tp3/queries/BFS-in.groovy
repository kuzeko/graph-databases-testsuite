#META:BFS_DEPTH=[2-5];SID=[0-10]

SID = System.env.get("SID").toInteger();
BFS_N = System.env.get("BFS_DEPTH").toInteger();


def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,DEPTH){
	v = g.V(id);


	t = System.nanoTime();
    count = v.repeat(in().where(without("x")).aggregate("x")).times(DEPTH).cap("x").next().size();
	exec_time = System.nanoTime() - t;

	//OUTPUT: DATABASE, DATASET,QUERY, SID_VALUE, ITERATION, ORDER, EXECUTION TIME(ns), RESULT, PARAMETER1 (Start_Node), PARAMETER2 (BFS-Depth)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), String.valueOf(count), String.valueOf(OBJECT_ARRAY[i]), String.valueOf(DEPTH)];
	println result_row.join(',');
}

//Depending on given NID: executes query for all of the nodes before ending or one node at a time
if (SID == NODE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
        execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,BFS_N);
        order_j++;
	}
} else {
    execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,BFS_N);
}
