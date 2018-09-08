#META:SID=[0-10]

SID = System.env.get("SID").toInteger(); 

def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID){
	
	query='MATCH ()-[n]-() WHERE ID(n)=';
	query+=id;
	query+=' RETURN n, id(n) as idn';
	t = System.nanoTime();        
	e=graph.cypher(query).select('idn').next();
	//e = g.E(id).next().id();
	exec_time = System.nanoTime() - t;

        //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(EDGE)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),e,String.valueOf(OBJECT_ARRAY[i].source)+" -- "+OBJECT_ARRAY[i].label+" -> "+ String.valueOf(OBJECT_ARRAY[i].target)];
	println result_row.join(',');
}

if (SID == EDGE_LID_ARRAY.size()) { 
	order_j = 1;
	for (i in RAND_ARRAY) {
        execute_query(g,EDGE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID);
       order_j++;
	}
} else { 
    execute_query(graph,g,EDGE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID);
}

//g.shutdown();


//[6,1,0,2,7]
//[6,1,0,2,7]
