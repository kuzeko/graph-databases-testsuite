#META:SID=[0-10]

SID = System.env.get("SID").toInteger();

def execute_query(graph,g,label,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID){

    
   /* println label.length();
    println label.size();
    println label;
    println label[15];*/
    
    /*query="START n=node(*) MATCH ()-[r:\"";
    query+=label;
    query+="\"]->() RETURN count(r) AS cnt";*/
    query="MATCH ()-[r]->() WHERE TYPE(r)=\"";
    query+=label;
    query+="\" RETURN count(r) AS cnt";
    t = System.nanoTime();
    cnt=graph.cypher(query).select('cnt').next();
    //cnt = g.E.hasLabel(label).count().next();
    count = String.valueOf(cnt)
    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(LABEL)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), count, String.valueOf(OBJECT_ARRAY[i])];
    println result_row.join(',');
};

if( SID == 10 ){ 
    // BATCH
    order_j = 1;
    for (i in RAND_ARRAY) {
        if(i < LABEL_ARRAY.size()) {
            execute_query(graph,g,LABEL_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,LABEL_ARRAY,SID);
            order_j++;
        }
    }
}else{
    if(SID < LABEL_ARRAY.size()){
        execute_query(graph,g,LABEL_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,LABEL_ARRAY,SID);
    } else {
        System.err.println("Empty LOOP");
    }
}
