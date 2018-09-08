#META:SID=[0-10]

SID = System.env.get("SID").toInteger();
def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,UID_NAME, UID_VALUE, SKIP_COMMIT){

    //System.err.println("V " + g.V().count().next())

	v = g.V(id).next();
	//retrieve node property hashmap
	property_map = v.properties().collect{ p -> p };
	//change that value
	//retrieve node edges as fromEdges(incoming to v) and toEdges (outgoing from v)
	index = 0;
	outdex = 0;

	outEdges = v.edges(Direction.OUT).collect{ e -> e }
	inEdges  = v.edges(Direction.IN).collect{ e -> e }

	//------- Node Insertion -----

	query="CREATE (n) SET n.";
	query+=UID_NAME;
	query+="=";
	query+=UID_VALUE;
	query+=" RETURN id(n) as idn";
	t = System.nanoTime();
	new_v=graph.cypher(query).select('idn').next();
	//new_v = g.addV().property(UID_NAME,UID_VALUE);
    for ( s in property_map ) {
        if(s.key() != UID_NAME){
	   query="MATCH (n) WHERE id(n)=";
	   query+=new_v;
	   query+=" SET n.";
	   query+=s.key();
	   query+="=\"";
	   query+=s.value();
           query+="\" RETURN id(n) as idn";
	   new_v=graph.cypher(query).select('idn').next();
           //new_v = new_v.property(s.key(), s.value());
        }
    }
    v = new_v;

	if(!SKIP_COMMIT){
		try {
			g.tx().commit();
            g.tx().open()
		} catch (MissingMethodException e) {
			System.err.println("Does not support g.commit(). Ignoring.");
		}
	}
	exec_time = System.nanoTime() - t;
    //def new_id = v.id()
    //System.err.println("Refind . . . " +  v.id())

    //v = g.V().has(UID_NAME,UID_VALUE).next();

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE_REPLICATED)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time)," ", String.valueOf(OBJECT_ARRAY[i]), String.valueOf(property_map.size())];
	println result_row.join(',');
	//------ Outgoing Edges Insertion

	t = System.nanoTime();
	/*outEdges.each{
        edge -> println edge.inVertex().id();
            v.addEdge(String.valueOf(edge.label), edge.inVertex());
            
	}*/
	outEdges.each{
	edge -> 
			query="MATCH (a), (b) WHERE id(b)=";
			query+=edge.inVertex().id();
			query+=" AND id(a)=";
			query+=v;
			query+=" CREATE (a)-[r:`";
			query+=edge.label;
			query+="`]->(b) return r";
			graph.cypher(query).next();
		
	}


	if(!SKIP_COMMIT){
		try {
			g.tx().commit();
            g.tx().open()
		} catch (MissingMethodException e) {
			System.err.println("Does not support g.commit(). Ignoring.");
		}
	}

	exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE),PARAMETER2(EDGES_INSERTED),PARAMETER3(EDGES_TYPE)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time)," ", String.valueOf(OBJECT_ARRAY[i]),String.valueOf(outEdges.size()), "in"];
	println result_row.join(',');


	//------ Incoming Edges Insertion

	t = System.nanoTime();
        /*inEdges.each{
          edge ->
            edge.outVertex().addEdge(String.valueOf(edge.label), v);
       }*/
	inEdges.each{
	edge -> 
			query="MATCH (a), (b) WHERE id(b)=";
			query+=edge.inVertex().id();
			query+=" AND id(a)=";
			query+=v;
			query+=" CREATE (a)<-[r:`";
			query+=edge.label;
			query+="`]-(b) return r";
			graph.cypher(query).next();
		
	}

	if(!SKIP_COMMIT){
		try {
			g.tx().commit();
		} catch (MissingMethodException e) {
			System.err.println("Does not support g.commit(). Ignoring.");
		}
	}
	exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE),PARAMETER2(EDGES_INSERTED),PARAMETER3(EDGES_TYPE)
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time)," ", String.valueOf(OBJECT_ARRAY[i]),String.valueOf(inEdges.size()), "out"];
	println result_row.join(',');

    //System.err.println("V " + g.V().count().next())
    //System.err.println("E " + g.V(new_id).bothE().count().next())

}

next_uid = f.infer_type(MAX_UID);


if (SID == NODE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
         execute_query(graph,g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f.uid_field,next_uid,SKIP_COMMIT);
         order_j++;
         next_uid = next_uid + 1;
	}
} else {
    execute_query(graph,g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f.uid_field,next_uid,SKIP_COMMIT);
}

//g.shutdown();
