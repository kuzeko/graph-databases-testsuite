SID = "0"


def execute_query_nodes(g,prop_name,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,f){
    t = System.nanoTime();
    found = false
    v = 0
    search = g.V(id)
	if(search.hasNext()){
         v = f.infer_type(search.next().value(prop_name))
         found= (v in  OBJECT_ARRAY)
    }
    if(!found){
        System.err.println("NODE LID " + id + " FOR " + v + "  MISSING!! ");
        System.exit(2);
    }

    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), id, v, found];
    println result_row.join(',');
}


def execute_query_edges(g,prop_name,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,f){
    t = System.nanoTime();
    found = false
    search = g.E(id)
    if(search.hasNext()){
         e = search.next()

         for( es in OBJECT_ARRAY){
            found = found || ( f.infer_type(e.outVertex().value(prop_name)).equals(f.infer_type(es.source))
                            && f.infer_type(e.inVertex().value(prop_name)).equals(f.infer_type(es.target))
                            && e.label().equals(es.label) )
            //System.err.println( [es.source, es.target, es.label].join(' ')   );
         }
    }

    if(!found){
        logM = e == null ? "NULL" : [e.outVertex().value(prop_name), e.inVertex().value(prop_name), e.label()].join(' ')
        System.err.println("EDGE LID " + id + " FOR " + logM +  "   MISSING!! ");
        System.exit(2);
    }

    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(EDGE)
    result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),id, String.valueOf(e), found];
    println result_row.join(',');

}



order_j = 1;
for (i in RAND_ARRAY) {
   execute_query_nodes(g,f.uid_field,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,f);
   order_j++;
}


order_j = 1;
for (i in RAND_ARRAY) {
  execute_query_edges(g,f.uid_field,EDGE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY,SID,f);
  order_j++;
}

