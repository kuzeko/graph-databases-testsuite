#META:INDEX=[0-10]

// Implementation does not guarantee to stop --> limit to x depth 
MAX_DEPTH = 10

def execute_query(g,id_source,id_destination,i_s,i_d,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID,DEPTH) {
    try {
        src_node = g.v(id_source);
        dst_node = g.v(id_destination)
        paths = []
        visited = [] as Set

        //Actual timed query
        t = System.nanoTime();
        src_node.as('x').both().except(visited).store(visited).loop('x'){ !it.object.equals(dst_node) && it.loops <= DEPTH}.retain([dst_node]).path().fill(paths)

        exec_time = System.nanoTime() - t;

        shortest_length = -1;
        if(paths.size() > 0){
            shortest_length = paths[0].size(); 
        }

        result_row = [ DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),String.valueOf(shortest_length), String.valueOf(OBJECT_ARRAY[i_s]), String.valueOf(OBJECT_ARRAY[i_d]), String.valueOf(DEPTH)];
        println result_row.join(',');
    } catch (Exception e) {
        System.err.println(e);
        System.exit(1);
    }
}

INDEX = System.env.get("INDEX").toInteger(); 

if (INDEX != RAND_ARRAY.size()) {
    SID = INDEX
    DID = (INDEX + 1) % NODE_LID_ARRAY.size()
    execute_query(g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID,MAX_DEPTH);
} else {
    order_j = 1;
    for (i=0; i < RAND_ARRAY.size();i++) {
       SID = i
       DID = (i + 1) % NODE_LID_ARRAY.size()
       execute_query(g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,INDEX,MAX_DEPTH);
       order_j++;
    }
}

//g.shutdown();
