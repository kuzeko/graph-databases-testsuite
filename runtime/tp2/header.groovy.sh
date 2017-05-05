#!/bin/bash

# Shared header:
#  connect to current engine, or open current database file from proper Class.
#  provides _g_ : greamlin graph object for querying.
#
#  Maintainer: Brugnara
#  Require bash > 4.x

# Imports should be common -> safe to use DATABASE_NAME
#    ["orientdb"]="import com.tinkerpop.blueprints.impls.orient.OrientGraph"
declare -A imports
imports=(
    ["neo4j"]=""
    ["orientdb"]="import com.tinkerpop.blueprints.impls.orient.OrientGraph"
    ["sparksee"]="import com.tinkerpop.blueprints.impls.sparksee.SparkseeGraph"
    ["titan"]="import com.thinkaurelius.titan.core.TitanFactory"
    ["arangodb"]="import com.arangodb.blueprints.*;import com.arangodb.blueprints.client.*"
)

# Commands should be common -> DATABASE
declare -A commands
commands=(
    ["gremlin-neo4j"]='def conf = ["node_cache_size":"100M","relationship_cache_size":"100M"]; g=new Neo4jGraph(DB_FILE, conf)'
    ["gremlin-orientdb"]='g = new OrientGraph("plocal:" + DB_FILE)'
    ["gremlin-sparksee"]='g = new SparkseeGraph(DB_FILE, "/runtime/confs/sparksee.cfg")'
    ["gremlin-titan"]='def TITAN_PROPERTIES=System.env.get("TITAN_PROPERTIES"); g=TitanFactory.open(TITAN_PROPERTIES);'
    ["gremlin-arangodb"]='g=new ArangoDBGraph("localhost", 8529, "myDB", "V", "E")'
)

if [[ -v DEBUG ]]; then
  (>&2 echo "$DATABASE  $DATABASE_NAME")
  (>&2 echo "${imports["$DATABASE_NAME"]}")
  (>&2 echo "${commands["$DATABASE"]}")
fi

cat<<EOF
// Data files
def DB_FILE="/srv/db"

// Current engine and dataset
def DATABASE_NAME = System.env.get("DATABASE_NAME");  // neo4j
def DATABASE = System.env.get("DATABASE");            // gremlin-neo4j | neo4j.yml
def DATASET = System.env.get("DATASET");              // path
def QUERY = System.env.get("QUERY");                  // path
def ITERATION = System.env.get("ITERATION");          // env variable


ESCAPE_LABELS = DATABASE == 'gremlin-orientdb'

def g = null                                          // graph

// private
def backtrack_waiting = 1
def success = false
def ex = null
EOF

echo "${imports["$DATABASE_NAME"]}"

cat<<EOF
for (i = 0; i < 5; i++) {
    try {
EOF

echo "${commands["$DATABASE"]}"

cat<<EOF
        success = true
        break
    } catch (Exception e) {
        ex = e
        Thread.sleep(backtrack_waiting * 1000)
        backtrack_waiting *= 2
    }
}

if (!success) {
  ex.printStackTrace()
  System.exit(1)
}
if (g == null) {
  System.err.println("graph object is not initialized")
  System.exit(1)
}
System.err.println("[NOW] $DATABASE  $QUERY");
EOF


cat<<EOF
// How many sample we need to identify?
N = 10;

// MAX_UID == next UID --> max(uid_field) + 1

UID_TYPE="string"
if (DATASET.contains('freebase')) {
 uid_field='freebaseid'
 MAX_UID=4611686018428691658
 UID_TYPE="numeric"
}
if (DATASET.contains('rhino')) {
 uid_field='rhinoId'
 MAX_UID=907366737573878978
 UID_TYPE="numeric"
} 
if (DATASET.contains('x_')) {
 uid_field='oid'
 MAX_UID=3000000
 UID_TYPE="numeric"
}
if (DATASET.contains('social_')) {
 uid_field='oid'
 MAX_UID=400000
 UID_TYPE="numeric"
}


// -- MUST_CONVERT
// always: always
// never: never
// possibly: decide instance by instance

// TODO: pass it from outside
switch (DATABASE) {
  case 'gremlin-sparksee':
    // It stores all the instances as the biggest one (per field):
    // (if exists one Long --> all Long)
    MUST_CONVERT = (MAX_UID as Long) > Integer.MAX_VALUE ? 'always' : 'never'
    break
  case 'gremlin-titan':
    MUST_CONVERT = 'possibly'
    break
  default:
    MUST_CONVERT = 'never'
}

System.err.println("Convert " + MUST_CONVERT);



System.err.println("UIDF: " + uid_field)


// define arrays
NODE_ARRAY = null
NODE_LID_ARRAY = null
EDGE_ARRAY = null
EDGE_LID_ARRAY = null
LABEL_ARRAY = null
SKIP_COMMIT = false

RAND_ARRAY = get_random_array(N, ITERATION.toInteger())

// depends on DATASET
lids_file = new File(System.env.get('LIDS_FILE')) // dataset, database

// http://www.groovy-tutorial.org/basic-json/
import groovy.json.JsonSlurper
import groovy.json.*
import java.nio.file.Paths
def jparser  = new JsonSlurper()

def infer_type(number) {
    if (UID_TYPE == "string"){
      return number as String
    }
    if(MUST_CONVERT != "never"){
        c = number as Long ;
        if(MUST_CONVERT == "possibly"){
           c = c >= Integer.MAX_VALUE ? c : c as Integer ;
        }
        return c;
    }

    return number;
}

def get_random_array(n, seed) {
    ra = [];
    for (i=0; i<n; i++) ra.add(i);
    Collections.shuffle(ra, new Random(seed));
    return ra;
}

EOF

if [[ "$QUERY" != *loader.groovy ]]; then
cat<<EOF

def lidm = [:]
//System.err.println(lids_file)
if (!lids_file.exists()) {
    System.err.println("LID File not Found! You need to re-run the sampler");
    System.exit(2);
}


System.err.println("Loading LID")
lidm = jparser.parseText(lids_file.text)

NODE_ARRAY = lidm.nodes
EDGE_ARRAY = lidm.edges
LABEL_ARRAY = lidm.labels

NODE_LID_ARRAY = lidm.nodes_lid
EDGE_LID_ARRAY = lidm.edges_lid
SKIP_COMMIT = lidm.commit

// -----------------------------------------------------------------------------


def get_next_bounded_tuple(lower_a, upper_a, lower_b, upper_b, index) {
    counter = 0
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


// -----------------------------------------------------------------------------
EOF

fi
