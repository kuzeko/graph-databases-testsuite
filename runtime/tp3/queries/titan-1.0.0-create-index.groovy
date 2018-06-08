import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.schema.SchemaAction;
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem;
import com.thinkaurelius.titan.core.schema.SchemaStatus;

t = System.nanoTime();

for( name in [f.uid_field , "test_common_property", "test_new_specific_property" ]){
try {
   mgmt = graph.openManagement()

   INDEXABLE_ATTRIBUTE=name  // Long

   def indx_name1 = INDEXABLE_ATTRIBUTE + '_INDEX'
   //System.err.println("Get Property for " + INDEXABLE_ATTRIBUTE )
   index_key1 = mgmt.getPropertyKey(INDEXABLE_ATTRIBUTE)

   //System.err.println("Create " + indx_name)
   mgmt.buildIndex(indx_name1, Vertex.class).addKey(index_key1).buildCompositeIndex();System.err.println("Commit");mgmt.commit();g.tx().commit()

   System.err.print("Wait for it . . . ")
   ManagementSystem.awaitGraphIndexStatus(graph, indx_name1).timeout(60, java.time.temporal.ChronoUnit.MINUTES).call()

   //System.err.println(" . . . done!")

   //System.err.print("Reindex . . . ")
   g.tx().rollback();mgmt = graph.openManagement();mgmt.updateIndex(mgmt.getGraphIndex(indx_name1), SchemaAction.REINDEX).get();mgmt.commit()

   System.err.println("Done index")

} finally {
   g.tx().rollback()
}
}

exec_time = System.nanoTime() - t;


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


