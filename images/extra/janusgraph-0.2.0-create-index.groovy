import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.schema.SchemaAction;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.janusgraph.core.schema.SchemaStatus;

try {
   t = System.nanoTime();
   mgmt = graph.openManagement()
   indx_name = f.uid_field + '_INDEX'
   //System.err.println("Get Property for " + f.uid_field )
   uid_key = mgmt.getPropertyKey(f.uid_field)

   //System.err.println("Create " + indx_name)
   mgmt.buildIndex(indx_name, Vertex.class).addKey(uid_key).buildCompositeIndex();System.err.println("Commit");mgmt.commit();g.tx().commit()

   //System.err.print("Wait for it . . . ")
   ManagementSystem.awaitGraphIndexStatus(graph, indx_name).timeout(60, java.time.temporal.ChronoUnit.MINUTES).call()

   //System.err.println(" . . . done!")

   //System.err.print("Reindex . . . ")
   g.tx().rollback();mgmt = graph.openManagement();mgmt.updateIndex(mgmt.getGraphIndex(indx_name), SchemaAction.REINDEX).get();mgmt.commit()

   //System.err.println("Done index")
   exec_time = System.nanoTime() - t;

} finally {
   g.tx().rollback()
}

INDEXABLE_ATTRIBUTE=f.uid_field

result_row = [ DATABASE, DATASET, QUERY,0, ITERATION, 0, String.valueOf(exec_time),INDEXABLE_ATTRIBUTE]
println result_row.join(',');

DEBUG = System.env.get("DEBUG") != null


try{
  if(DEBUG){
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

    result_row = [ DATABASE, DATASET, QUERY,0, ITERATION, 0, String.valueOf(exec_time1), String.valueOf(count1), String.valueOf(exec_time2), String.valueOf(count2),INDEXABLE_ATTRIBUTE]
    println result_row.join(',');
  }
} catch (Exception e) {
  e.printStackTrace()
}


