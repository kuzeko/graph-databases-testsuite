#META:BFS_DEPTH=[2-5];SID=[0-10]

SID = System.env.get("SID").toInteger(); 
BFS_N = System.env.get("BFS_DEPTH").toInteger();

def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,LABEL_ARRAY,SID,DEPTH){
	v = g.V(id);
	String str=String.join(" OR ", *LABEL_ARRAY);
	t = System.nanoTime();
	query ="MATCH (b)-[r:`"
	query+=str;
	query+="`*..";
	query+=DEPTH;
	query+="]->(a) WHERE id(a)=";
	query+=id;
        query+=" RETURN count(*) as cnt";
        count=graph.cypher(query).select('cnt').next();
    //count = v.repeat(in(*LABEL_ARRAY).where(without("x")).aggregate("x")).times(DEPTH).cap("x").next().size();
	exec_time = System.nanoTime() - t;

	//OUTPUT: DATABASE, DATASET,QUERY, SID_VALUE, ITERATION, ORDER, EXECUTION TIME(ns), RESULT, PARAMETER1 (Start_Node), PARAMETER2 (BFS-Depth)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), String.valueOf(count), String.valueOf(OBJECT_ARRAY[i]), String.valueOf(DEPTH)];
	println result_row.join(',');
}

//Depending on given NID: executes query for all of the nodes before ending or one node at a time
if (SID == NODE_LID_ARRAY.size()) { 
	order_j = 1;
	for (i in RAND_ARRAY) {
        execute_query(graph,g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,LABEL_ARRAY,SID,BFS_N);
        order_j++;
	}
} else {
    execute_query(graph,g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,LABEL_ARRAY,SID,BFS_N);
}

//g.shutdown();


//not testable because error in original gremlin query
