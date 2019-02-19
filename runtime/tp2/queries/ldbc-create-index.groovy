def DEBUG = System.env.get("DEBUG") != null
INDEXABLE_ATTRIBUTE=uid_field  // Long
PROPERTY_NAME1= "xname";   // String
PROPERTY_NAME2= "firstName"; // String

t = System.nanoTime();
g.createKeyIndex(INDEXABLE_ATTRIBUTE,Vertex.class)

if(!SKIP_COMMIT){  try {  g.commit(); } catch (MissingMethodException e) {
     System.err.println("Does not support g.commit(). Ignoring.");
}}

g.createKeyIndex(PROPERTY_NAME1,Vertex.class)

if(!SKIP_COMMIT){  try {  g.commit(); } catch (MissingMethodException e) {
     System.err.println("Does not support g.commit(). Ignoring.");
}}


g.createKeyIndex(PROPERTY_NAME2,Vertex.class)

if(!SKIP_COMMIT){  try {  g.commit(); } catch (MissingMethodException e) {
     System.err.println("Does not support g.commit(). Ignoring.");
}}

exec_time = System.nanoTime() - t;


result_row = [ DATABASE, DATASET, QUERY,0, ITERATION, 0, String.valueOf(exec_time),INDEXABLE_ATTRIBUTE ]
println result_row.join(',');

if(DEBUG){
  System.err.println("TEST INDEX ON " + INDEXABLE_ATTRIBUTE)

  t = System.nanoTime();
  count1 = 0
  for(int i=0; i < NODE_ARRAY.size(); i++){
     NODE_ID = infer_type(NODE_ARRAY[i])
     count1 += g.V.has(INDEXABLE_ATTRIBUTE, infer_type(NODE_ID)).count();
  }
  exec_time1 = System.nanoTime() - t;

  System.err.println(" SEARCH NON EXIST " + INDEXABLE_ATTRIBUTE)
  t = System.nanoTime();
  nEx = MAX_UID
  count2 = 0
  for(int i=0; i < NODE_ARRAY.size(); i++){
    nEx = infer_type(nEx+1)
    count2 += g.V().has(INDEXABLE_ATTRIBUTE, nEx).count();
  }
  exec_time2 = System.nanoTime() - t;

  result_row = [ DATABASE, DATASET, QUERY,0, ITERATION, 0, String.valueOf(exec_time1), String.valueOf(count1), String.valueOf(exec_time2), String.valueOf(count2),INDEXABLE_ATTRIBUTE]

  println result_row.join(',');
}

