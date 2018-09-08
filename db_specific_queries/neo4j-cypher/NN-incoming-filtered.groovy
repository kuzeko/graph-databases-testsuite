#META:SID=[0-10]

SID = System.env.get("SID").toInteger(); 

//--------------------------------------------------------------------------------------------

def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,LABEL_ARRAY){
	v = g.V(id);
String str=String.join("`|`", *LABEL_ARRAY);
    
	query="MATCH (n)<-[r:`";
	query+=str;
	query+="`]-() WHERE id(n)=";
	query+=id;
	query+=" RETURN count(r) as cnt";
	t = System.nanoTime();	
	count=graph.cypher(query).select("cnt").next();
	//count = v.inE(*LABEL_ARRAY).count().next();
	exec_time = System.nanoTime() - t;

	//OUTPUT: DATABASE, DATASET, QUERY, SID_VALUE, ITERATION, NODE_ORDER, EXECUTION_TIME, OUTPUT, PARAMETER1(Start_Node), PARAMETER2(Label), LABEL_ORDER 
	//Added LABEL_ORDER AT THE END, even though it is not a parameter in the strict sense
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),String.valueOf(count), String.valueOf(OBJECT_ARRAY[i])];
	println result_row.join(',');
}

if (SID == NODE_LID_ARRAY.size()) { 
	order_j = 1;
	for (i in RAND_ARRAY) {
       execute_query(graph,g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,LABEL_ARRAY);
       order_j++;
	}
} else {
    execute_query(graph,g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,LABEL_ARRAY);
}

//g.shutdown();
