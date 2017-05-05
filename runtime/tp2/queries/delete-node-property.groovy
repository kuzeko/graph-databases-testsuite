SID = System.env.get("SID").toInteger();
def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,property_name){
    v = g.v(id);
    t = System.nanoTime();
    v.removeProperty(property_name);
    if (!SKIP_COMMIT) {
        try {
            g.commit();
        } catch (MissingMethodException e) {
            System.err.println("Does not support g.commit(). Ignoring.");
        }
    }
    exec_time = System.nanoTime() - t;
    size = v.map().size();
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),size, String.valueOf(OBJECT_ARRAY[i]), String.valueOf(property_name)];
    println result_row.join(',');
}
if (SID == NODE_LID_ARRAY.size()) {
    order_j = 1;
    for (i in RAND_ARRAY) {
        execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,uid_field);
        order_j++;
    }
} else {
    execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,uid_field);
}
