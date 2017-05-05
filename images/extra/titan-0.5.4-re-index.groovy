import com.thinkaurelius.titan.core.TitanFactory;
def TITAN_PROPERTIES=System.env.get("TITAN_PROPERTIES");
def DATASET = System.env.get("DATASET");

def UID_TYPE="string"
if (DATASET.contains('freebase')) {
 uid_field='freebaseid'
 UID_TYPE="numeric"
}
if (DATASET.contains('rhino')) {
 uid_field='rhinoId'
 UID_TYPE="numeric"
} 
if (DATASET.contains('x_')) {
 uid_field='oid'
 UID_TYPE="numeric"
}
if (DATASET.contains('social_')) {
 uid_field='oid'
 UID_TYPE="numeric"
}

def indx_name = uid_field + '_INDEX'

System.err.println("Reindex . . .")
// Run a Titan-Hadoop job to reindex (replace Murmur3 with your actual partitioner)
pt = "org.apache.cassandra.dht.Murmur3Partitioner" // The default
TitanIndexRepair.cassandraRepair(TITAN_PROPERTIES, indx_name, "", pt)

// Enable the index
System.err.println("Enable")
def g=TitanFactory.open(TITAN_PROPERTIES);
mgmt = g.getManagementSystem()
mgmt.updateIndex(mgmt.getGraphIndex(indx_name), SchemaAction.ENABLE_INDEX);
mgmt.commit()

// Check the status -- should be ENABLED
mgmt = g.getManagementSystem()
desc = mgmt.getPropertyKey(uid_field)
st = mgmt.getGraphIndex(indx_name).getIndexStatus(desc)
System.err.println("Status " + st)
mgmt.rollback()
g.shutdown()

System.err.println("Done index")

System.exit(0);
