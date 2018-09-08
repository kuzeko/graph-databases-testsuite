#META:INDEX=[0-10]

def execute_query(graph,g,id_source,id_destination,i_s,i_d,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,LABEL_ARRAY,SID) {

    v = g.V(id_source);
    String str=String.join(" OR ", *LABEL_ARRAY);
    //Actual timed query
    t = System.nanoTime();
    query="MATCH (a),(b), p = shortestPath((a)-[r:`";
    query+=str;
    query+="`*]-(b)) WHERE ID(a)=";
    query+=id_source;
    query+=" AND ID(b)=";
    query+=id_destination;
    query+=" RETURN length(p) as lngh";
    l=graph.cypher(query).select('lngh'); 
    //l = v.repeat(both(*LABEL_ARRAY).where(without("x")).aggregate("x")).until(hasId(id_destination)).limit(1).path().count(local);
    if(l.hasNext()){
      l = l.next() + 1;
    } else {
      l = -1
    }
    exec_time = System.nanoTime() - t;


    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),String.valueOf(l), String.valueOf(OBJECT_ARRAY[i_s]), String.valueOf(OBJECT_ARRAY[i_d])];
    println result_row.join(',');
}


INDEX = System.env.get("INDEX").toInteger();

if (INDEX != RAND_ARRAY.size()) {
    SID = INDEX
    DID = (INDEX + 1) % NODE_LID_ARRAY.size()
    execute_query(graph,g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,LABEL_ARRAY,SID);
} else {
    order_j = 1;
    for (i=0; i < RAND_ARRAY.size();i++) {
        SID = i
        DID = (i + 1) % NODE_LID_ARRAY.size()
        execute_query(graph,g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,LABEL_ARRAY,INDEX);
        order_j++;
    }
}
//g.shutdown();


//6, 2, 4, 4, 3
//5, 1, 3, 3, 2
//6, 2, 4, 4, 3
