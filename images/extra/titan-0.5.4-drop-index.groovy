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

try{
mgmt = g.getManagementSystem()
def indx_name = uid_field + '_INDEX'
System.err.println("Get Index for " + indx_name )
u_index = mgmt.getGraphIndex(indx_name)

System.err.print("Disable " + indx_name + "  . . . " )
mgmt.updateIndex(u_index, SchemaAction.DISABLE_INDEX)
System.err.println(" commit! " )
mgmt.commit()


/* REMOVE IS NOT IMPLMENTED
mgmt = g.getManagementSystem()
u_index = mgmt.getGraphIndex(indx_name)
System.err.print("REMOVE " + indx_name + "  . . . " )
mgmt.updateIndex(u_index, SchemaAction.REMOVE_INDEX)
System.err.println(" commit! " )
mgmt.commit()
*/

} catch (Exception e){
  e.printStackTrace();

}
System.err.println("Done index")

System.exit(0);
