import com.thinkaurelius.titan.core.TitanFactory;
def TITAN_PROPERTIES=System.env.get("TITAN_PROPERTIES");
def g=TitanFactory.open(TITAN_PROPERTIES);

mgmt = g.getManagementSystem()

// Loop on props and create them
def f = new File("/props.txt");
f.eachLine() { line ->
    System.err.println("processing: " + line)
    String[] parts = line.split(",");
    switch (parts[1]) {
        case "string":
            mgmt.makePropertyKey(parts[0]).dataType(String.class).cardinality(Cardinality.SINGLE).make();
            break
        case "long":
            mgmt.makePropertyKey(parts[0]).dataType(Long.class).cardinality(Cardinality.SINGLE).make();
            break
        case "integer":
            mgmt.makePropertyKey(parts[0]).dataType(Integer.class).cardinality(Cardinality.SINGLE).make();
            break
        case "double":
            mgmt.makePropertyKey(parts[0]).dataType(Double.class).cardinality(Cardinality.SINGLE).make();
            break
        case "float":
            mgmt.makePropertyKey(parts[0]).dataType(Float.class).cardinality(Cardinality.SINGLE).make();
            break
        default:
            System.err.println("Unhandled property type " + parts[1])
            System.exit(1)
    }
}

System.err.println("Create custom prop")
mgmt.makePropertyKey('test_common_property').dataType(String.class).cardinality(Cardinality.SINGLE).make();
mgmt.makePropertyKey('test_specific_property').dataType(String.class).cardinality(Cardinality.SINGLE).make();
mgmt.makePropertyKey('test_new_specific_property').dataType(String.class).cardinality(Cardinality.SINGLE).make();

System.err.println("Done with prop")

System.err.println("Creating labels")
f = new File("/labels.txt");
f.eachLine() { line ->
        mgmt.makeEdgeLabel(line).multiplicity(Multiplicity.MULTI).make()
}
mgmt.makeEdgeLabel('test_label').multiplicity(Multiplicity.MULTI).make()
System.err.println("Done with labels")

mgmt.commit();

System.err.println("Done schema")

System.exit(0);
