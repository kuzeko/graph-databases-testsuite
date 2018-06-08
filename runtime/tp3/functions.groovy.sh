#!/bin/bash

# Maintainer: Lissandrini

# Provides utility functions, expecially for sampler.

#Reserved for sampler
echo "class Functs {"

cat<<EOF

def uid_type
def max_uid
def uid_field
def must_convert
def skip_commit

public Functs(UID_TYPE, MAX_UID, UID_FIELD, MUST_CONVERT, SKIP_COMMIT) {
 uid_type =  UID_TYPE
 max_uid =  MAX_UID
 uid_field =  UID_FIELD
 must_convert = MUST_CONVERT
 skip_commit = SKIP_COMMIT
}

EOF

if [[ "$QUERY" == *loader.groovy ]]  || [[ "$QUERY" == *sampler.groovy ]]; then
cat<<EOF

// -----------------------------------------------------------------------------



def sample_nodes(g, n) {
    return g.V().sample(n).values(uid_field).collect { nid -> infer_type(nid) }
}

def sample_edges(g, n) {
    return g.E.sample(n).collect { edge -> [
        source: infer_type(edge.outVertex().value(uid_field)),
        target: infer_type(edge.inVertex().value(uid_field)),
        label: edge.label() as String
    ]}
}


def sample_labels(g, n) {
    return g.E.label.dedup().sample(n).collect { ld -> ld as String }
}



def get_nodes_lid(g, nodes) {
    if (System.env.get('ALT_GET_LID') == null) {
        return nodes.collect { node ->
            def nId = infer_type(node);
            g.V.has(uid_field, nId).next().id() as String
        }
    }

    def nids = nodes.collect { node ->
        def nId = infer_type(node) ;
        return nId
    }

    def coll = []
    def i = 0
    def p = g.V()
    while ( p.hasNext() ){
       def n = p.next()
       if( nids.contains(n.values(uid_field).next()) ){
         coll.add(n.id() as String)
       }
       i = i + 1;
       if(i % 10000 == 1){
         System.err.print(i +", ")
       }
    }
    System.err.println(".")

    return coll
}

def get_edges_lid(g, edges) {
    System.err.println("SEARCH " + edges.size())
    if (System.env.get('ALT_GET_LID') == null) {

        return edges.collect { edge ->
            def sidId = infer_type(edge.source);
            def tidId = infer_type(edge.target);

            def e = g.V().has(uid_field, sidId).outE(edge.label).where(inV().has(uid_field, tidId)).next()
            return e.id() as String
       }
   }

   def eis = edges.collect { edge ->
       def sidId = infer_type(edge.source);
       def tidId = infer_type(edge.target);
       return [sidId , edge.label, tidId]
   }

    System.err.println("Search")
    def coll = []
    def p = g.V()
    def stime = System.currentTimeMillis()*1.0
    def fcount =0;
    while ( p.hasNext() ){
       def n = p.next()
       def id = n.id()
       def nFid = n.values(uid_field).next()
       def found =  eis.find { it[0].equals(nFid) }
       if( found != null ){
         def e = n.edges(Direction.OUT, found[1]).find{ infer_type(it.inVertex().property(uid_field).value()).equals(infer_type(found[2]))}
         if(e  == null) {
                  System.err.println( found[0] + " "  + nFid +  " ->  " + found[2] )
         }
         coll.add(e.id() as String)
         def etime = System.currentTimeMillis()*1.0
         fcount+=1
         System.err.println(fcount + "   " + ((etime - stime)/1000) + " secs" )

       }
    }

    return coll



}


def set_nodes_lid_labels(g, nodes_lids) {
    // Artificially introduce properties
    nodes_lids.eachWithIndex { lid, i ->
        def node = g.V(lid)
        if (node == null) {
            System.err.println("Node lid not found " + lid)
        } else {
            node.property('test_common_property',  "test_value").property('test_specific_property', "test_value_" + i).iterate()
        }
    }


    if(!skip_commit){
       try {
          g.tx().commit();
       } catch (MissingMethodException e) {
          System.err.println("Does not support g.tx().commit(). Ignoring.");
       }
    }
}

def set_edges_lid_labels(g, edges_lids) {
    // Artificially introduce properties
    edges_lids.eachWithIndex { lid, i ->
        def edge = g.E(lid)
        if (edge == null) {
            System.err.println("Edge lid not found " + lid)
        } else {
            edge.property('test_common_property'   , "test_value" ).property('test_specific_property' , "test_value_" + i).iterate()
        }
    }

    if(!skip_commit){
       try {
          g.tx().commit();
       } catch (MissingMethodException e) {
          System.err.println("Does not support g.tx().commit(). Ignoring.");
       }
    }
}


// -----------------------------------------------------------------------------
EOF

fi


cat<<EOF

def get_next_bounded_tuple(lower_a, upper_a, lower_b, upper_b, index) {
    def counter = 0
    for (int i = lower_a; i <= upper_a; i++) {
       for (int j = Math.max(i+1, lower_b); j <= upper_b; j++){
          if (counter == index) {
             return new Tuple(i, j)
          } else {
             counter++
          }
       }
    }

    // see return above
    throw new Exception("get_next_bounded_tuple index out of domain");
}


def get_random_array(n, seed) {
    def ra = [];
    for (int i=0; i<n; i++) ra.add(i);
    Collections.shuffle(ra, new Random(seed));
    return ra;
}

def infer_type(number) {
    if (uid_type == "string"){
      return number as String
    }
    if(must_convert){
        def c = number as Long ;
        c = c > Integer.MAX_VALUE ? c : c as Integer ;
        return c;
    }
    return number;
}

}

EOF
