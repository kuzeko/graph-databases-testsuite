SID = System.env.get("SID").toInteger(); 

def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,prop_name){

    v = g.V(id);
    vNode = g.V(id).next();
    nodes = []
    t = System.nanoTime();  
    v.as('a').out().out().where(neq('a')).not(__.in().hasId(id)).limit(10).properties(prop_name).value().fill(nodes);
    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), String.valueOf(OBJECT_ARRAY[i]), String.valueOf(nodes)];
    println result_row.join(',');
}

if (SID == NODE_LID_ARRAY.size()) { 
    order_j = 1;
    for (i in RAND_ARRAY) {
        execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f.uid_field);
        order_j++;
    }
} else {
    execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f.uid_field);
}

//g.shutdown();

