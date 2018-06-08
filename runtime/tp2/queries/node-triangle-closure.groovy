#META:SID=[0-10]

SID = System.env.get("SID").toInteger();

def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,prop_name){

    v = g.v(id);
    vNode = g.v(id);
    nodes = []
    t = System.nanoTime();
    v.out().out().except([vNode]).filter{!it.in.retain([vNode]).hasNext()}[0..<10].map(prop_name).fill(nodes);
    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), String.valueOf(OBJECT_ARRAY[i]), String.valueOf(nodes)];
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

//g.shutdown();

