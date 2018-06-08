#META:SID=[0-10]

SID = System.env.get("SID").toInteger(); 

//--------------------------------------------------------------------------------------------

def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,LABEL_ARRAY){
	v = g.V(id);
    t = System.nanoTime();
	count = v.bothE(*LABEL_ARRAY).count().next();
	exec_time = System.nanoTime() - t;

	//OUTPUT: DATABASE, DATASET, QUERY, SID_VALUE, ITERATION, NODE_ORDER, EXECUTION_TIME, OUTPUT, PARAMETER1(Start_Node), PARAMETER2(Label), LABEL_ORDER 
	//Added LABEL_ORDER AT THE END, even though it is not a parameter in the strict sense
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),String.valueOf(count), String.valueOf(OBJECT_ARRAY[i])];
	println result_row.join(',');
}

if (SID == NODE_LID_ARRAY.size()) { 
	order_j = 1;
	for (i in RAND_ARRAY) {
       execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,LABEL_ARRAY);
       order_j++;
	}
} else {
    execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,LABEL_ARRAY);
}

//g.shutdown();
