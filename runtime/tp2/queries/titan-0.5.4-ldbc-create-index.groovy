import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.hadoop.TitanIndexRepair;

INDEXABLE_ATTRIBUTE=uid_field  // Long
PROPERTY_NAME1= "xname";   // String
PROPERTY_NAME2= "firstName"; // String


t = System.nanoTime();
mgmt = g.getManagementSystem()
def indx_name1 = INDEXABLE_ATTRIBUTE + '_INDEX'
def indx_name2 = PROPERTY_NAME1 + '_INDEX'
def indx_name3 = PROPERTY_NAME2 + '_INDEX'

System.err.println("Get Property for " + indx_name1 + ' ' + indx_name2 + ' ' + indx_name3 )
indx_name1_key = mgmt.getPropertyKey(INDEXABLE_ATTRIBUTE)
indx_name2_key = mgmt.getPropertyKey(PROPERTY_NAME1)
indx_name3_key = mgmt.getPropertyKey(PROPERTY_NAME2)

System.err.print("Create " + indx_name1_key + " . . . " + indx_name1)
mgmt.buildIndex(indx_name1, Vertex.class).addKey(indx_name1_key).buildCompositeIndex()
System.err.print("Create " + indx_name2_key + " . . . " + indx_name2)
mgmt.buildIndex(indx_name2, Vertex.class).addKey(indx_name2_key).buildCompositeIndex()
System.err.print("Create " + indx_name2_key + " . . . " +  indx_name3)
mgmt.buildIndex(indx_name3, Vertex.class).addKey(indx_name3_key).buildCompositeIndex()

//System.err.println(" Commit")
mgmt.commit()
g.commit()

// Block until the SchemaStatus transitions from INSTALLED to REGISTERED
registered = false
//System.err.print("SchemaStatus transitions from INSTALLED to REGISTERED: ")
while (!registered) {
    Thread.sleep(500L)
    //System.err.print(" . ")
    registered = true
    mgmt = g.getManagementSystem()
    idx  = mgmt.getGraphIndex(indx_name1)
    for (k in idx.getFieldKeys()) {
        s = idx.getIndexStatus(k)
        registered &= s.equals(SchemaStatus.REGISTERED)
    }

    idx  = mgmt.getGraphIndex(indx_name2)
    for (k in idx.getFieldKeys()) {
        s = idx.getIndexStatus(k)
        registered &= s.equals(SchemaStatus.REGISTERED)
    }

    idx  = mgmt.getGraphIndex(indx_name3)
    registered = true
    for (k in idx.getFieldKeys()) {
        s = idx.getIndexStatus(k)
        registered &= s.equals(SchemaStatus.REGISTERED)
    }
    mgmt.rollback()
}
System.err.println("End while")

// Run a Titan-Hadoop job to reindex (replace Murmur3 with your actual partitioner)
pt = "org.apache.cassandra.dht.Murmur3Partitioner" // The default
TITAN_PROPERTIES=System.env.get("TITAN_PROPERTIES");
TitanIndexRepair.cassandraRepair(TITAN_PROPERTIES, indx_name1, "", pt)
TitanIndexRepair.cassandraRepair(TITAN_PROPERTIES, indx_name2, "", pt)
TitanIndexRepair.cassandraRepair(TITAN_PROPERTIES, indx_name3, "", pt)

// Enable the index
System.err.println("Enable")
//def g=TitanFactory.open(TITAN_PROPERTIES);
mgmt = g.getManagementSystem();mgmt.updateIndex(mgmt.getGraphIndex(indx_name1), SchemaAction.ENABLE_INDEX);mgmt.commit()
mgmt = g.getManagementSystem();mgmt.updateIndex(mgmt.getGraphIndex(indx_name2), SchemaAction.ENABLE_INDEX);mgmt.commit()
mgmt = g.getManagementSystem();mgmt.updateIndex(mgmt.getGraphIndex(indx_name3), SchemaAction.ENABLE_INDEX);mgmt.commit()


// Check the status -- should be ENABLED
mgmt = g.getManagementSystem();desc = mgmt.getPropertyKey(INDEXABLE_ATTRIBUTE);st = mgmt.getGraphIndex(indx_name1).getIndexStatus(desc)
mgmt = g.getManagementSystem();desc = mgmt.getPropertyKey(PROPERTY_NAME1);st = mgmt.getGraphIndex(indx_name2).getIndexStatus(desc)
mgmt = g.getManagementSystem();desc = mgmt.getPropertyKey(PROPERTY_NAME2);st = mgmt.getGraphIndex(indx_name3).getIndexStatus(desc)
//System.err.println("Status " + st + " Done index")
exec_time = System.nanoTime() - t;


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





