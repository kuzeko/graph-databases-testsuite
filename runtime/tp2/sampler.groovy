
samples_file = new File(System.env.get('SAMPLES_FILE')) // dataset

if(ESCAPE_LABELS){
    System.err.println("All labels are being escaped: / is now __")
}

def samples = [:]
if (!samples_file.exists()) {
    System.err.println("Sampling File not Found! Creating")
    System.err.println("Sampling nodes")
    samples.nodes  = sample_nodes(g, N)
    System.err.println(samples.nodes)

    System.err.println("Sampling edges")
    samples.edges  = sample_edges(g, N)
    System.err.println(samples.edges)

    System.err.println("Sampling labels")
    samples.labels = sample_labels(g, N)
    System.err.println(samples.labels)

    samples_file << (new JsonBuilder(samples)).toString()
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
    g.commit();
} catch (MissingMethodException e) {
    System.err.println("Does not support g.commit(). Ignoring.");
    SKIP_COMMIT = true
}

def lidm = [:]
System.err.println(lids_file)
if (!lids_file.exists()) {
    System.err.println("Retrieving LID")

    System.err.println("Retrieving LID -> nodes")
    lidm.nodes_lid = get_nodes_lid(g, NODE_ARRAY)
    System.err.println(lidm.nodes_lid)


    System.err.println("Retrieving LID -> edges")
    lidm.edges_lid = get_edges_lid(g, EDGE_ARRAY)
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
set_nodes_lid_labels(g, lidm.nodes_lid)
System.err.println("SET Edges")
set_edges_lid_labels(g, lidm.edges_lid)
System.err.println("Done!")



// -----------------------------------------------------------------------------
def sample_nodes(g, n) {
    return g.V.shuffle().next(n)[uid_field].collect { nid -> infer_type(nid) }
}

def sample_labels(g, n) {
    return g.E.label.dedup().shuffle().next(n)
}

def sample_edges(g, n) {
    return g.E.shuffle().next(n).collect { edge -> [
        source: infer_type(edge.outV[uid_field].next()),
        target: infer_type(edge.inV[uid_field].next()),
        label: edge.label
    ]}
}


def get_nodes_lid(g, nodes) {
    if (System.env.get('ALT_GET_LID') == null) {
        return nodes.collect { node ->
            nId = infer_type(node);
            g.V.has(uid_field, nId).next().id as String
        }
    }

    def nids = nodes.collect { node ->
        def nId = infer_type(node) ;
        System.err.println(nId)
        return nId
    }
    System.err.println("Search")
    def coll = []
    def i = 0
    def p = g.V()
    while ( p.hasNext() ){
       def n = p.next()
       if( nids.contains(n[uid_field]) ){
         coll.add(n.id as String)
       }
       i++;
       if(i % 50000 == 1){
         System.err.println(i)
       }
    }
    return coll
}

def get_edges_lid(g, edges) {
    return edges.collect { edge ->
        sidId = infer_type(edge.source);
        sid = g.V.has(uid_field, sidId).next();
        
        tidId =infer_type(edge.target);
        tid = g.V.has(uid_field, tidId).next();
        sid.outE(edge.label).as('selected').inV.retain([tid]).back('selected').next().id as String
    }
}


def set_nodes_lid_labels(g, nodes_lids) {
    // Artificially introduce properties
    nodes_lids.eachWithIndex { lid, i ->
        node = g.v(lid)
        if (node == null) {
            System.err.println("Node lid not found " + lid)
        } else {
            node.test_common_property   = "test_value"
            node.test_specific_property = "test_value_" + i
        }
    }


    if(!SKIP_COMMIT){
       try {
          g.commit();
       } catch (MissingMethodException e) {
          System.err.println("Does not support g.commit(). Ignoring.");
       }
    }
}

def set_edges_lid_labels(g, edges_lids) {
    // Artificially introduce properties
    edges_lids.eachWithIndex { lid, i ->
    edge = g.e(lid)
        if (edge == null) {
            System.err.println("Edge lid not found " + lid)
        } else {
            edge.test_common_property   = "test_value"
            edge.test_specific_property = "test_value_" + i
        }
    }

    if(!SKIP_COMMIT){
       try {
          g.commit();
       } catch (MissingMethodException e) {
          System.err.println("Does not support g.commit(). Ignoring.");
       }
    }
}

// MUST DO IT (IN LOADER)
g.shutdown()
