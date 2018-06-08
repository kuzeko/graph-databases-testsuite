import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.hadoop.TitanIndexRepair;

t = System.nanoTime();
mgmt = g.getManagementSystem()
def indx_name = uid_field + '_INDEX'
//System.err.println("Get Property for " + uid_field )
uid_key = mgmt.getPropertyKey(uid_field)

//System.err.print("Create " + indx_name + " . . . ")
mgmt.buildIndex(indx_name, Vertex.class).addKey(uid_key).buildCompositeIndex()

//System.err.println(" Commit")
mgmt.commit()
g.commit()

// Block until the SchemaStatus transitions from INSTALLED to REGISTERED
registered = false
//System.err.print("SchemaStatus transitions from INSTALLED to REGISTERED: ")
while (!registered) {
    Thread.sleep(500L)
    //System.err.print(" . ")

    mgmt = g.getManagementSystem()
    idx  = mgmt.getGraphIndex(indx_name)
    registered = true
    for (k in idx.getFieldKeys()) {
        s = idx.getIndexStatus(k)
        registered &= s.equals(SchemaStatus.REGISTERED)
    }
    mgmt.rollback()
}

// Run a Titan-Hadoop job to reindex (replace Murmur3 with your actual partitioner)
pt = "org.apache.cassandra.dht.Murmur3Partitioner" // The default
TITAN_PROPERTIES=System.env.get("TITAN_PROPERTIES");
TitanIndexRepair.cassandraRepair(TITAN_PROPERTIES, indx_name, "", pt)

// Enable the index
//System.err.println("Enable")
//def g=TitanFactory.open(TITAN_PROPERTIES);
mgmt = g.getManagementSystem();mgmt.updateIndex(mgmt.getGraphIndex(indx_name), SchemaAction.ENABLE_INDEX);mgmt.commit()


// Check the status -- should be ENABLED
mgmt = g.getManagementSystem();desc = mgmt.getPropertyKey(uid_field);st = mgmt.getGraphIndex(indx_name).getIndexStatus(desc)
//System.err.println("Status " + st + " Done index")
exec_time = System.nanoTime() - t;

INDEXABLE_ATTRIBUTE=uid_field

result_row = [ DATABASE, DATASET, QUERY,0, ITERATION, 0, String.valueOf(exec_time),INDEXABLE_ATTRIBUTE]
println result_row.join(',');

def DEBUG = System.env.get("DEBUG") != null

if(DEBUG){
  System.err.println("TEST INDEX ON " + INDEXABLE_ATTRIBUTE)

  t = System.nanoTime();
  count1 = 0
  for(int i=0; i < NODE_ARRAY.size(); i++){
     NODE_ID = infer_type(NODE_ARRAY[i])
     count1 += g.V.has(INDEXABLE_ATTRIBUTE, infer_type(NODE_ID)).count();
  }
  exec_time1 = System.nanoTime() - t;

  System.err.println(" SEARCH NON EXIST ON " + INDEXABLE_ATTRIBUTE)
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





