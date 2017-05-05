#!/bin/bash

#

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
    ["neo4j"]=''
    ["orientdb"]='import com.tinkerpop.blueprints.impls.orient.OrientGraph;'
    ["sparksee"]='import com.tinkerpop.blueprints.impls.sparksee.SparkseeGraph;'
    ["infinitegraph"]='import com.tinkerpop.blueprints.pgm.impls.ig.*;'
    ["titan"]='import com.thinkaurelius.titan.core.TitanFactory;'
    ["blazegraph"]='c = ((GroovyClassLoader) this.class.classLoader).parseClass(new File("/tmp/BlazegraphRepositoryProvider.groovy"));'
    ["arangodb"]='import com.arangodb.blueprints.*;import com.arangodb.blueprints.client.*;'
    ["neo4j-tp3"]=''
    ["titan-tp3"]='import com.thinkaurelius.titan.core.TitanFactory; import org.apache.log4j.*; SugarLoader.load() '

)

# Commands should be common -> DATABASE
declare -A commands
commands=(
    ["gremlin-neo4j"]='conf = ["node_cache_size":"100M","relationship_cache_size":"100M"]; g=new Neo4jGraph(DB_FILE, conf)'
    ["gremlin-orientdb"]='g = new OrientGraph("plocal:" + DB_FILE)'
    ["gremlin-sparksee"]='g = new SparkseeGraph(DB_FILE, "/runtime/confs/sparksee.cfg")'
    ["gremlin-titan"]='TITAN_PROPERTIES=System.env.get("TITAN_PROPERTIES"); g=TitanFactory.open(TITAN_PROPERTIES);'
    ["gremlin-arangodb"]='g=new ArangoDBGraph("localhost", 8529, "myDB", "V", "E")'
    ["gremlin-blazegraph"]='repo = BlazegraphRepositoryProvider.open(DB_FILE + ".jnl", true); graph = BlazeGraphEmbedded.open(repo); g = graph.traversal();'
    ["gremlin-neo4j-tp3"]='graph = Neo4jGraph.open(DB_FILE); g = graph.traversal()'
    ["gremlin-titan-tp3"]='LogManager.getRootLogger().setLevel(Level.WARN); TITAN_PROPERTIES=System.env.get("TITAN_PROPERTIES"); graph=TitanFactory.open(TITAN_PROPERTIES); g = graph.traversal()'

)

if [[ -v DEBUG ]]; then
  (>&2 echo "$DATABASE  $DATABASE_NAME")
  (>&2 echo "${imports["$DATABASE_NAME"]}")
  (>&2 echo "${commands["$DATABASE"]}")
fi


echo -e ${imports["$DATABASE_NAME"]}

cat<<EOF

// http://www.groovy-tutorial.org/basic-json/
import groovy.json.JsonSlurper
import groovy.json.*
import java.nio.file.Paths

EOF

if [[ -v DEBUG ]]; then
   echo DEBUG=true
fi
. /runtime/tp3/functions.groovy.sh

cat<<EOF


RAND_ARRAY =[]

// Data files
DB_FILE="/srv/db"

// Current engine and dataset
DATABASE_NAME = System.env.get("DATABASE_NAME");  // neo4j
DATABASE = System.env.get("DATABASE");            // gremlin-neo4j | neo4j.yml
DATASET = System.env.get("DATASET");              // path
QUERY = System.env.get("QUERY");                  // path
ITERATION = System.env.get("ITERATION");          // env variable

ESCAPE_LABELS = DATABASE == 'gremlin-orientdb'


// -- MUST_CONVERT
// always: always
// never: never
// possibly: decide instance by instance

// TODO: pass it from outside
ESCAPE_LABELS = DATABASE == 'gremlin-orientdb'
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


// How many sample we need to identify?
N = 10;

UID_FIELD=''
UID_TYPE="string"
MAX_UID=''

// define arrays
NODE_ARRAY = null
NODE_LID_ARRAY = null
EDGE_ARRAY = null
EDGE_LID_ARRAY = null
LABEL_ARRAY = null
SKIP_COMMIT = false



g = null                                          // graph

// private
backtrack_waiting = 1
success = false
ex = null


for (int i = 0; i < 5; i++) {
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

if (DATASET.contains('freebase')) {
 UID_FIELD='freebaseid'
 MAX_UID=4611686018428691658
 UID_TYPE="numeric"
}
if (DATASET.contains('rhino')) {
 UID_FIELD='rhinoId'
 MAX_UID=907366737573878978
 UID_TYPE="numeric"
}
if (DATASET.contains('x_')) {
 UID_FIELD='oid'
 MAX_UID=3000000
 UID_TYPE="numeric"
}
if (DATASET.contains('social_')) {
 UID_FIELD='oid'
 MAX_UID=400000
 UID_TYPE="numeric"
}


f = new Functs(UID_TYPE, MAX_UID, UID_FIELD, MUST_CONVERT, SKIP_COMMIT)
RAND_ARRAY = f.get_random_array(N, ITERATION.toInteger())


// depends on DATASET
lids_file = new File(System.env.get('LIDS_FILE')) // dataset, database

jparser  = new JsonSlurper();
EOF



if [[ "$QUERY" != *loader.groovy ]]; then
cat<<EOF

lidm = [:]
System.err.println(lids_file)
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

EOF

fi
