import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.schema.SchemaAction;
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem;
import com.thinkaurelius.titan.core.schema.SchemaStatus;



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

mgmt = g.openManagement()
def indx_name = uid_field + '_INDEX'
System.err.println("Get Property for " + uid_field )
uid_key = mgmt.getPropertyKey(uid_field)

System.err.println("Create " + indx_name)
mgmt.buildIndex(indx_name, Vertex.class).addKey(uid_key).buildCompositeIndex()

System.err.println("Commit")
mgmt.commit()
g.tx().commit()

System.err.print("Wait for it . . . ")
//mgmt.awaitGraphIndexStatus(g, indx_name).call()
ManagementSystem.awaitGraphIndexStatus(g, indx_name).timeout(60, java.time.temporal.ChronoUnit.MINUTES).call()

System.err.println(" . . . done!")

//System.err.print("Rollback . . . ")
//mgmt.rollback();
//System.err.print(" . . 2 . . ")
//g.tx().rollback();


System.err.print("Reindex . . . ")
g.tx().rollback();mgmt = g.openManagement();mgmt.updateIndex(mgmt.getGraphIndex(indx_name), SchemaAction.REINDEX).get();mgmt.commit()
//System.err.print("Commit . . . ")
//System.err.println(" done! ")

//System.err.print(" . . . ENABLE . . . ")
//ManagementSystem.awaitGraphIndexStatus(g,indx_name).status(SchemaStatus.ENABLED).timeout(60, java.time.temporal.ChronoUnit.MINUTES).call()

System.err.println("Done index")

System.exit(0);
