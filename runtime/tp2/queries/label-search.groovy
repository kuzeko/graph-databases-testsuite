#META:SID=[0-10]

SID = System.env.get("SID").toInteger();

def execute_query(g,label,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID){

    t = System.nanoTime();
    cnt = g.E.has('label', label).count();
    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(LABEL)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), String.valueOf(cnt), String.valueOf(OBJECT_ARRAY[i])];
    println result_row.join(',');
}

if (SID == 10) { // BATCH
    order_j = 1;
    for (i in RAND_ARRAY) {
        if(i < LABEL_ARRAY.size()) {
            execute_query(g,LABEL_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,LABEL_ARRAY,SID);
            order_j++;
        }
    }
} else {
    if(SID < LABEL_ARRAY.size()) {
        execute_query(g,LABEL_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,LABEL_ARRAY,SID);
    }
}
