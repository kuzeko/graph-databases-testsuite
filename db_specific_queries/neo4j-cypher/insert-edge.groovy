#META:INDEX=[0-10]

def execute_query(graph,g,id_source,id_destination,i_s,i_d,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,SKIP_COMMIT){

	to = g.V(id_destination).next();
	//c = g.V(id_source).addE("test_label").to(to).count().next();
	query="MATCH (a), (b) WHERE ID(a)=";
	query+=id_source;
	query+=" AND ID(b)=";
	query+=id_destination;
	query+=" CREATE (a)-[r:test_label]->(b) RETURN a";
	t = System.nanoTime();
	c=graph.cypher(query).next();
        //c = g.V(id_source).next().addEdge("test_label", to);
	if(!SKIP_COMMIT){
		try {
			g.tx().commit();
		} catch (MissingMethodException e) {
			System.err.println("Does not support g.tx().commit(). Ignoring.");
		}
	}

	exec_time = System.nanoTime() - t;

	//DATABASE,DATASET,QUERY,SID_VALUE, ITERATION, ORDER, TIME, OUTPUT, PARAMETER1(SOURCE_NODE), PARAMETER2(DESTINATION_NODE)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time)," ", String.valueOf(OBJECT_ARRAY[i_s]),String.valueOf(OBJECT_ARRAY[i_d])];
	println result_row.join(',');
}

INDEX = System.env.get("INDEX").toInteger();

if (INDEX != RAND_ARRAY.size()) {
    SID = INDEX
    DID = (INDEX + 1) % NODE_LID_ARRAY.size()
    execute_query(graph,g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,SKIP_COMMIT);
} else {
	order_j = 1;
	for (i=0; i < RAND_ARRAY.size();i++) {
             SID = i
             DID = (i + 1) % NODE_LID_ARRAY.size()	  
             execute_query(g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,INDEX,SKIP_COMMIT);
             order_j++;
	}
}

//g.shutdown();

//6880808548-6878808548
//6880808548-6878808548
