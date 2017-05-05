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

try{
mgmt = g.openManagement()
def indx_name = uid_field + '_INDEX'
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
