#META:INDEX=[0-10]

LABEL_TO_ADD= "test_label";
PROPERTY_NAME= "test_specific_property";
PROPERTY_VALUE = "test_value_";


def execute_query(g,id_source,id_destination,i_s,i_d,ORDER_j,DATABASE,DATASET,QUERY,ITERATION,OBJECT_ARRAY,SID, LABEL, PROP_NAME, PROP_VAL,SKIP_COMMIT) {

    to = g.V(id_destination).next();
    t = System.nanoTime();
    pmap = [ PROP_NAME, PROP_VAL ]
    //g.V(id_source).addE(LABEL).to(to).property(pmap).iterate();
    g.V(id_source).next().addEdge(LABEL, to, *pmap);
    if(!SKIP_COMMIT){
        try {
            g.tx().commit();
        } catch (MissingMethodException e) {
            System.err.println("Does not support g.tx().commit(). Ignoring.");
        }
    }
    exec_time = System.nanoTime() - t;

    //DATABASE,DATASET,QUERY,SID_VALUE, ITERATION, ORDER, TIME, OUTPUT, PARAMETER1(SOURCE_NODE), PARAMETER2(DESTINATION_NODE)
    result_row = [DATABASE, DATASET, QUERY, String.valueOf(SID), ITERATION, String.valueOf(ORDER_j), String.valueOf(exec_time),'', String.valueOf(OBJECT_ARRAY[i_s]),String.valueOf(OBJECT_ARRAY[i_d])];
    println result_row.join(',');
}

INDEX = System.env.get("INDEX").toInteger();

if (INDEX != RAND_ARRAY.size()) {
    SID = INDEX
    DID = (INDEX + 1) % NODE_LID_ARRAY.size()
    execute_query(g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],0,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,SID, LABEL_TO_ADD, PROPERTY_NAME, (PROPERTY_VALUE+(INDEX+1)),SKIP_COMMIT);
} else {
    order_j = 1;

    for (i=0; i < RAND_ARRAY.size();i++) {
       SID = i
       DID = (i+ 1) % NODE_LID_ARRAY.size()
       execute_query(g,NODE_LID_ARRAY[RAND_ARRAY[SID]],NODE_LID_ARRAY[RAND_ARRAY[DID]],RAND_ARRAY[SID],RAND_ARRAY[DID],order_j,DATABASE,DATASET,QUERY,ITERATION,NODE_ARRAY,RAND_ARRAY.size(), LABEL_TO_ADD, PROPERTY_NAME, (PROPERTY_VALUE+(1+i)),SKIP_COMMIT);
       order_j++;
    }
}

//g.shutdown();
