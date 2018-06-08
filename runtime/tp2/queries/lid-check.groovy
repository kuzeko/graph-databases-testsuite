def execute_query_nodes(g,prop_name,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY){

    t = System.nanoTime();
    found = false
    v = 0
    search = g.v(infer_type(id)).gather().scatter()
    if(search.hasNext()){
         v = infer_type(search.next().getProperty(prop_name))
	 for (value in OBJECT_ARRAY) {
		if (v == infer_type(value)) {
			found = true;
			break;
		}
	}
    }
    if(!found){
        System.err.println("NODE LID " + id + " FOR " + v + "  MISSING!! ");
        System.exit(2);
    }
	exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,0,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(NODE)
	result_row = [ DATABASE, DATASET, QUERY, 0, ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time), id, v, found];
	println result_row.join(',');
}


def execute_query_edges(g,prop_name,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY){
    t = System.nanoTime();

    found = false
    e = null

    try {
        search = g.e(id).gather().scatter()
        if(search.hasNext()){
            e = search.next()
            for( es in OBJECT_ARRAY){
                found = found || ( infer_type(e.outV().property(prop_name).next()).equals(infer_type(es.source)) && infer_type(e.inV().property(prop_name).next()).equals(infer_type(es.target)) && e.label().next().equals(es.label))
                //System.err.println( [es.source, es.target, es.label].join(' ')  );
            }
        } 
    } catch(Exception e) {
        found = false;
    }
    if(!found){
        logM = e == null ? "NULL" : [e.outV().property(prop_name).next(), e.inV().property(prop_name).next(), e.label().next()].join(' ')
        System.err.println("EDGE LID " + id + " FOR " + logM +  "   MISSING!! ");
        System.exit(2);
    }

	exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,0,,ITERATION,ORDER,TIME,OUTPUT,PARAMETER1(EDGE)
	result_row = [ DATABASE, DATASET, QUERY, 0, ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),id, String.valueOf(e), found];
	println result_row.join(',');
}

order_j = 1;
for (i in RAND_ARRAY) {
   execute_query_nodes(g,uid_field,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY);
   order_j++;
}


order_j = 1;
for (i in RAND_ARRAY) {
  execute_query_edges(g,uid_field,EDGE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,EDGE_ARRAY);
  order_j++;
}
