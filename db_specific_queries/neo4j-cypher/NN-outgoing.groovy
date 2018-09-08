#META:SID=[0-10]

SID = System.env.get("SID").toInteger(); 

def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID){
	v = g.V(id);

	query="MATCH (n)-[r]->() WHERE id(n)=";
	query+=id;
	query+=" RETURN count(r) as cnt";
	t = System.nanoTime();
	count=graph.cypher(query).select("cnt").next();
	//count = v.outE.count().next();
	exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),String.valueOf(count), String.valueOf(OBJECT_ARRAY[i])];
	println result_row.join(',');
}

if (SID == NODE_LID_ARRAY.size()) { 
	order_j = 1;
	for (i in RAND_ARRAY) {
        execute_query(graph,g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID);
        order_j++;
	}
} else {
    execute_query(graph,g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID);
}

//g.shutdown();
