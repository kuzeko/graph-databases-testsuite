#META:SID=[0-10]

SID = System.env.get("SID").toInteger();

def execute_query(graph,g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID, PROP_NAME, PROP_VAL, SKIP_COMMIT){
    //v = g.V(id).next();
    //pp = v.property(PROP_NAME);

    query="MATCH (n) WHERE ID(n)=";
    query+=id;
    query+=" SET n.";
    query+=PROP_NAME;
    query+=" = \"";
    query+=PROP_VAL;
    query+="\" RETURN n.";
    query+=PROP_NAME;
    t = System.nanoTime();
    v = graph.cypher(query).next();	
    //v = v.property(PROP_NAME, PROP_VAL);

    if(!SKIP_COMMIT){
        try {
            g.tx().commit();
        } catch (MissingMethodException e) {
            System.err.println("Does not support g.tx().commit(). Ignoring.");
        }
    }
    exec_time = System.nanoTime() - t;
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),String.valueOf(v), String.valueOf(OBJECT_ARRAY[i]), String.valueOf(PROP_NAME), String.valueOf(PROP_VAL)];
    println result_row.join(',');
}


PROPERTY_NAME= "test_common_property";
PROPERTY_VALUE = "test_value_new";

if (SID == NODE_LID_ARRAY.size()) {
    order_j = 1;
    for (i in RAND_ARRAY) {
        execute_query(graph,g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,PROPERTY_NAME,PROPERTY_VALUE,SKIP_COMMIT);
        order_j++;
    }
} else {
     execute_query(graph,g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,PROPERTY_NAME,PROPERTY_VALUE,SKIP_COMMIT);
}
