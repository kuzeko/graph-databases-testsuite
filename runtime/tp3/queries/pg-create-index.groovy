INDEXABLE_ATTRIBUTE=f.uid_field  // Long
PROPERTY_NAME1= "test_common_property";   // String
PROPERTY_NAME2= "test_new_specific_property"; // String


DEBUG = System.env.get("DEBUG") != null


//System.err.println("INDEX ON " + INDEXABLE_ATTRIBUTE)
t = System.nanoTime();
// Last value  (0l) is used by PG to determine type of index (long)!
graph.createVertexLabeledIndex("vertex", INDEXABLE_ATTRIBUTE, 0l);
graph.createVertexLabeledIndex("vertex", PROPERTY_NAME1, "x");
graph.createVertexLabeledIndex("vertex", PROPERTY_NAME2, "x");
g.tx().commit();
exec_time = System.nanoTime() - t;
//System.err.println("DONE INDEX ON " + INDEXABLE_ATTRIBUTE)

result_row = [ DATABASE, DATASET, QUERY,0, ITERATION, 0, String.valueOf(exec_time),INDEXABLE_ATTRIBUTE]
println result_row.join(',');


try{
  if(DEBUG){
    g = graph.traversal()
    System.err.println("TEST INDEX ON " + INDEXABLE_ATTRIBUTE)
    t = System.nanoTime();
    count1 = 0
    for(int i=0; i < NODE_ARRAY.size(); i++){
      NODE_ID = f.infer_type(NODE_ARRAY[i])
      count1 += g.V().hasLabel('vertex').has(INDEXABLE_ATTRIBUTE, NODE_ID).count().next();
    }
    exec_time1 = System.nanoTime() - t;

    System.err.println(" SEARCH NON EXIST " + INDEXABLE_ATTRIBUTE)
    t = System.nanoTime();
    count2 = 0
    nEx = f.max_uid
    for(int i=0; i < NODE_ARRAY.size(); i++){
      nEx = f.infer_type(nEx+1)
      count2 += g.V().hasLabel('vertex').has(INDEXABLE_ATTRIBUTE, nEx).count().next();
    }
    exec_time2 = System.nanoTime() - t;

    result_row = [ DATABASE, DATASET, QUERY,0, ITERATION, 0, String.valueOf(exec_time1), String.valueOf(count1), String.valueOf(exec_time2), String.valueOf(count2), INDEXABLE_ATTRIBUTE]
    println result_row.join(',');
  }
} catch (Exception e) {
  e.printStackTrace()
}
