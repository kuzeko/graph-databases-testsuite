SID = System.env.get("SID").toInteger();
def execute_query(g,id,i,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,UID_NAME, UID_VALUE, SKIP_COMMIT){
	v = g.V(id).next();
	property_map = v.properties().collect{ p -> p };
	index = 0;
	outdex = 0;
	outEdges = v.edges(Direction.OUT).collect{ e -> e }
	inEdges  = v.edges(Direction.IN).collect{ e -> e }
	t = System.nanoTime();
	new_v = g.addV().property(UID_NAME,UID_VALUE);
    for ( s in property_map ) {
        if(s.key() != UID_NAME){
           new_v = new_v.property(s.key(), s.value());
        }
    }
    v = new_v.next()
	if(!SKIP_COMMIT){
		try {
			g.tx().commit();
            g.tx().open()
		} catch (MissingMethodException e) {
			System.err.println("Does not support g.commit(). Ignoring.");
		}
	}
	exec_time = System.nanoTime() - t;
    def new_id = v.id()
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time)," ", String.valueOf(OBJECT_ARRAY[i]), String.valueOf(property_map.size())];
	println result_row.join(',');
	t = System.nanoTime();
	outEdges.each{
        edge ->
            v.addEdge(String.valueOf(edge.label), edge.inVertex());
	}
	if(!SKIP_COMMIT){
		try {
			g.tx().commit();
            g.tx().open()
		} catch (MissingMethodException e) {
			System.err.println("Does not support g.commit(). Ignoring.");
		}
	}
	exec_time = System.nanoTime() - t;
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time)," ", String.valueOf(OBJECT_ARRAY[i]),String.valueOf(outEdges.size()), "in"];
	println result_row.join(',');
	t = System.nanoTime();
        inEdges.each{
          edge ->
            edge.outVertex().addEdge(String.valueOf(edge.label), v);
       }
	if(!SKIP_COMMIT){
		try {
			g.tx().commit();
		} catch (MissingMethodException e) {
			System.err.println("Does not support g.commit(). Ignoring.");
		}
	}
	exec_time = System.nanoTime() - t;
	result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time)," ", String.valueOf(OBJECT_ARRAY[i]),String.valueOf(inEdges.size()), "out"];
	println result_row.join(',');
}
next_uid = f.infer_type(MAX_UID);
if (SID == NODE_LID_ARRAY.size()) {
	order_j = 1;
	for (i in RAND_ARRAY) {
         execute_query(g,NODE_LID_ARRAY[i],i,order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,UID_FIELD,next_uid,SKIP_COMMIT);
         order_j++;
         next_uid = next_uid + 1;
	}
} else {
    execute_query(g,NODE_LID_ARRAY[SID],SID,0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,UID_FIELD,next_uid,SKIP_COMMIT);
}
