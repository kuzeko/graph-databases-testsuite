#META:SID=[0-10]

SID = System.env.get("SID").toInteger(); 

def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,SKIP_COMMIT){
    v = g.V(id).next();

    query="MATCH (n) WHERE ID(n) = ";
    query+=id;
    query+=" DETACH DELETE n";
    t = System.nanoTime();
    graph.cypher(query).iterate();
    //c = v.remove();


    if(!SKIP_COMMIT){
        try {
            g.tx().commit();
        } catch (MissingMethodException e) {
            System.err.println("Does not support g.tx().commit(). Ignoring.");
        }
    }
    exec_time = System.nanoTime() - t;
    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),0, String.valueOf(OBJECT_ARRAY[i])];
    println result_row.join(',');
}

if (SID == NODE_LID_ARRAY.size()) { 
    order_j = 1;
    for (i in RAND_ARRAY) {
        execute_query(graph,g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,SKIP_COMMIT);
        order_j++;
    }
} else {
    execute_query(graph,g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,SKIP_COMMIT);
}

//g.shutdown();

//tried with .next, nothing, iterate. Always returns Transaction was marked as successful, but unalbe to commit transaction so rolled back. Next returns nullpointer
