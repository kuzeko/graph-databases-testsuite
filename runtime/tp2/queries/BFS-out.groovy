SID = System.env.get("SID").toInteger();
BFS_N = System.env.get("BFS_DEPTH").toInteger();
def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,DEPTH){
	v = g.v(id);
	visited = [v] as Set;
	t = System.nanoTime();
        count = v.as('x').out().except(visited).store(visited).loop('x'){ it.loops <= DEPTH}{true}.count();
        exec_time = System.nanoTime() - t;
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), String.valueOf(count), String.valueOf(OBJECT_ARRAY[i]), String.valueOf(DEPTH)];
	println result_row.join(',');
}
if (SID == NODE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
	    execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,BFS_N);
	    order_j++;
	}
} else {
	 execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,BFS_N);
}
