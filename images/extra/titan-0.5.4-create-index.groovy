import com.thinkaurelius.titan.core.TitanFactory;
def TITAN_PROPERTIES=System.env.get("TITAN_PROPERTIES");
def g=TitanFactory.open(TITAN_PROPERTIES);
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

mgmt = g.getManagementSystem()
def indx_name = uid_field + '_INDEX'
System.err.println("Get Property for " + uid_field )
uid_key = mgmt.getPropertyKey(uid_field)

System.err.print("Create " + indx_name + " . . . ")
mgmt.buildIndex(indx_name, Vertex.class).addKey(uid_key).buildCompositeIndex()

System.err.println(" Commit")
mgmt.commit()
g.commit()

// Block until the SchemaStatus transitions from INSTALLED to REGISTERED
registered = false
before = System.currentTimeMillis()
System.err.print("SchemaStatus transitions from INSTALLED to REGISTERED: ")
while (!registered) {
    Thread.sleep(500L)
    System.err.print(" . ")

    mgmt = g.getManagementSystem()
    idx  = mgmt.getGraphIndex(indx_name)
    registered = true
    for (k in idx.getFieldKeys()) {
        s = idx.getIndexStatus(k)
        registered &= s.equals(SchemaStatus.REGISTERED)
    }
    mgmt.rollback()
}
System.err.println("Index REGISTERED in " + (System.currentTimeMillis() - before) + " ms")


// Enable the index
System.err.println("Enable")
//def g=TitanFactory.open(TITAN_PROPERTIES);
mgmt = g.getManagementSystem()
mgmt.updateIndex(mgmt.getGraphIndex(indx_name), SchemaAction.ENABLE_INDEX);
mgmt.commit()

// Check the status -- should be ENABLED
mgmt = g.getManagementSystem()
desc = mgmt.getPropertyKey(uid_field)
st = mgmt.getGraphIndex(indx_name).getIndexStatus(desc)
System.err.println("Status " + st)


//mgmt.rollback()
g.shutdown()

System.err.println("Done index")

System.exit(0);
