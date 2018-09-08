#META:BFS_DEPTH=[2-5];SID=[0-10]

SID = System.env.get("SID").toInteger();
BFS_N = System.env.get("BFS_DEPTH").toInteger();


def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,DEPTH){
	v = g.V(id);
	t = System.nanoTime();
        // OLD COUNT count = v.repeat(both().simplePath()).times(DEPTH).emit().count().next();
	query ="MATCH (b)-[*..";
    	query+=DEPTH+1;
    	query+="]-(a) WHERE id(a)=";
    	query+=id;
    	query+=" RETURN count(distinct b) as cnt";
	//query ="MATCH (a)-[*1..";
        //query+=(DEPTH+1);
	//query+="]-(b) WHERE id(a)=";
	//query+=id;
	//query+=" RETURN count(b) as cnt";
	count=graph.cypher(query).select("cnt").next();
    	//count = v.repeat(both().where(without("x")).aggregate("x")).times(DEPTH).cap("x").dedup().next().size();
	exec_time = System.nanoTime() - t;

	//OUTPUT: DATABASE, DATASET,QUERY, SID_VALUE, ITERATION, ORDER, EXECUTION TIME(ns), RESULT, PARAMETER1 (Start_Node), PARAMETER2 (BFS-Depth)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), String.valueOf(count), String.valueOf(OBJECT_ARRAY[i]), String.valueOf(DEPTH)];
	println result_row.join(',');
}

//Depending on given NID: executes query for all of the nodes before ending or one node at a time
if (SID == NODE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
       execute_query(graph, g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,BFS_N);
       order_j++;
	}
} else {
	execute_query(graph,g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,BFS_N);
}



//my_count(depth) vs new_count returns 6, 8 instead of 8, 12

//my_count(depth+1) vs new_count returns 10, 12 instead of 8,12

//my_count(depth) vs old_count seems to be the same [6,8,6,4,4]

//my_count(distinct depth) returns [6, 8, 6, 4, 4]

//my_count (depth) returns [6, 8, 6, 4, 4]

//old_count returns [6, 8, 6, 4, 4]

//new_count returns [8, 12, ...]

// my_count (depth+1) returns [10, 12, ...]

//my_count (distinct depth+1) returns [8, 9, 9, 7, 5
