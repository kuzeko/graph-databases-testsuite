import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.schema.SchemaAction;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.janusgraph.core.schema.SchemaStatus;

JANUS_PROPERTIES=System.env.get("JANUS_PROPERTIES");
g=JanusGraphFactory.open(JANUS_PROPERTIES);
DATASET = System.env.get("DATASET"); 


UID_TYPE="string"
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

try{
mgmt = g.openManagement()
indx_name = uid_field + '_INDEX'
System.err.println("Get Index for " + indx_name )
u_index = mgmt.getGraphIndex(indx_name)

System.err.print("Disable " + indx_name + "  . . . " )
mgmt.updateIndex(u_index, SchemaAction.DISABLE_INDEX).get()
System.err.println(" commit! " )
mgmt.commit()
g.tx().commit()

System.err.print("Await " + indx_name + "  . . . " )
ManagementSystem.awaitGraphIndexStatus(g, indx_name).status(SchemaStatus.DISABLED).timeout(60, java.time.temporal.ChronoUnit.MINUTES).call()

mgmt = g.openManagement()
u_index = mgmt.getGraphIndex(indx_name)
System.err.print("REMOVE " + indx_name + "  . . . " )
mgmt.updateIndex(u_index, SchemaAction.REMOVE_INDEX).get()
System.err.println(" commit! " )
mgmt.commit()


} catch (Exception e){
  e.printStackTrace();

}
System.err.println("Done index")

System.exit(0);
