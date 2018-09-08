#META:SID=[0-10]

SID = System.env.get("SID").toInteger(); 

def execute_query(graph, g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID, SKIP_COMMIT){

    //e = g.E(id).next();

    //c = e.remove();
    query="MATCH ()-[r]-() WHERE ID(r) = ";
    query+=id;
    query+=" DELETE r";
    t = System.nanoTime();
    c=graph.cypher(query);

    if(!SKIP_COMMIT){
        try {
            graph.tx().commit();
        } catch (MissingMethodException e) {
            System.err.println("Does not support g.tx().commit(). Ignoring.");
        }
    }

    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(EDGE)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),,String.valueOf(OBJECT_ARRAY[i].source)+" -- "+OBJECT_ARRAY[i].label+" -> "+ String.valueOf(OBJECT_ARRAY[i].target)];
    println result_row.join(',');
}

if (SID == EDGE_LID_ARRAY.size()) {
    order_j = 1;
    for (i in RAND_ARRAY) {
        execute_query(graph, g,EDGE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID, SKIP_COMMIT);
        order_j++;
    }
} else {
    execute_query(graph, g,EDGE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID, SKIP_COMMIT);
}
