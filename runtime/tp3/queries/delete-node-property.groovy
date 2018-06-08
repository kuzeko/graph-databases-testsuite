#META:SID=[0-10]

SID = System.env.get("SID").toInteger();

def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,PROP_NAME,SKIP_COMMIT){
    v = g.V(id).next();

    t = System.nanoTime();
    v.property(PROP_NAME).remove();
    if (!SKIP_COMMIT) {
        try {
            g.tx().commit();
        } catch (MissingMethodException e) {
            System.err.println("Does not support g.tx().commit(). Ignoring.");
        }
    }
    exec_time = System.nanoTime() - t;
    //size = g.V(id).properties().size()

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE),PARAMETER2(PROPERTY)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),'', String.valueOf(OBJECT_ARRAY[i]), String.valueOf(PROP_NAME)];
    println result_row.join(',');
}

if (SID == NODE_LID_ARRAY.size()) {
    order_j = 1;
    for (i in RAND_ARRAY) {
        execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f.uid_field,SKIP_COMMIT);
        order_j++;
    }
} else {
    execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f.uid_field,SKIP_COMMIT);
}

//g.shutdown();
