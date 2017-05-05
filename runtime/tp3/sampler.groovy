samples_file = new File(System.env.get('SAMPLES_FILE')) // dataset

if(ESCAPE_LABELS){
    System.err.println("All labels are being escaped: / is now __")
}

samples = [:]
if (!samples_file.exists()) {
    System.err.println("Sampling File not Found! Creating")
    System.err.println("Sampling nodes")
    samples.nodes  = f.sample_nodes(g, N)
    System.err.println(samples.nodes)

    System.err.println("Sampling edges")
    samples.edges  = f.sample_edges(g, N)
    System.err.println(samples.edges)

    System.err.println("Sampling labels")
    samples.labels = f.sample_labels(g, N)
    System.err.println(samples.labels)
    System.err.println(samples)
    try{
      samples_file << (new JsonBuilder(samples)).toString() 
    } catch(Exception e){
           System.err.println("We couldn't write the file") 
           System.err.println(e) 
           System.exit(4)
    }
 
} else {
  System.err.println("Loading PRECOMPUTED Sampling")
  samples = jparser.parseText(samples_file.text)
}


// ESCAPE_LABEL  is For OrientDB: waiting patch fix
// https://github.com/orientechnologies/orientdb/issues/6577
NODE_ARRAY = samples.nodes
EDGE_ARRAY = samples.edges.collect { edge -> [
        source: edge.source,
        target: edge.target,
        label: ESCAPE_LABELS ? edge.label.replace("/","__") : edge.label
    ]};

LABEL_ARRAY = samples.labels.collect { label ->  ESCAPE_LABELS ? label.replace("/","__") : label }

try {
    g.tx().commit();
} catch (UnsupportedOperationException e) {
    System.err.println("Does not support g.tx().commit(). Ignoring.");
    SKIP_COMMIT = true
} catch (MissingMethodException e) {
    System.err.println("Does not support g.tx().commit(). Ignoring.");
    SKIP_COMMIT = true
}

lidm = [:]
System.err.println(lids_file)
if (!lids_file.exists()) {
    System.err.println("Retrieving LID")

    System.err.println("Retrieving LID -> nodes")
    lidm.nodes_lid = f.get_nodes_lid(g, NODE_ARRAY)
    System.err.println(lidm.nodes_lid)


    System.err.println("Retrieving LID -> edges")
    lidm.edges_lid = f.get_edges_lid(g, EDGE_ARRAY)
    System.err.println(lidm.edges_lid)


    System.err.println("Retrieving LID -> done")

    lidm.commit=SKIP_COMMIT

    lidm.nodes=NODE_ARRAY
    lidm.edges=EDGE_ARRAY
    lidm.labels=LABEL_ARRAY

    System.err.println("Writing LID File")

    System.err.println(lidm)

    lids_file << (new JsonBuilder(lidm).toString())
} else {
    System.err.println("LID Already Existing: INCONSISTENT STATE")
    System.exit(2);
}

NODE_LID_ARRAY = lidm.nodes_lid
EDGE_LID_ARRAY = lidm.edges_lid


System.err.println("SET Nodes & Edges Custom Labels")
System.err.println("SET Nodes")
f.set_nodes_lid_labels(g, NODE_LID_ARRAY)
System.err.println("SET Edges")
f.set_edges_lid_labels(g, EDGE_LID_ARRAY)
System.err.println("Done!")
