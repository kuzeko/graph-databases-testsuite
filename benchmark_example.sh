#!/bin/bash

source .venv/bin/activate

python test.py \
  -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx120G -XX:+UseG1GC' \
  -r 3 \
  -s settings_example.json \
  -i dbtrento/gremlin-arangodb \
  -i dbtrento/gremlin-pg \
  -i dbtrento/gremlin-neo4j \
  -i dbtrento/gremlin-neo4j-tp3 \
  -i dbtrento/gremlin-sparksee

python test.py \
  -e JAVA_OPTIONS='-Xms4g -Xmx20g -XX:+UseG1GC -Dstorage.diskCache.bufferSize=102400' \
  -r 3 \
  -s settings_example.json \
  -i dbtrento/gremlin-orientdb

python test.py \
  -e TITAN_JAVA_OPTS='-Xms4G -Xmx120G -XX:+UseG1GC -Dcassandra.jmx.local.port=9999 -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false' \
  -r 3 \
  -s settings_example.json \
  -i dbtrento/gremlin-titan \
  -i dbtrento/gremlin-titan-tp3 \
  -i dbtrento/gremlin-janus-tp3

python test.py \
  -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx120G -XX:MaxDirectMemorySize=60000m -XX:+UseG1GC' \
  -r 3 \
  -s settings_example.json \
  -i dbtrento/gremlin-blazegraph


# -----------------------------------------------------------------------------
# Test DB specifc queries implementations.

# FIXME: results will be reported as 'neo4j-tp3'
python test.py \
  -e JAVA_OPTIONS='-Xms1G -Xmn128M -Xmx120G -XX:+UseG1GC' \
  -r 3 \
  -i dbtrento/gremlin-neo4j-tp3 \
  -s cypher_reduced_set.json \
  -v "$(pwd)"/db_specific_queries/neo4j-cypher:/runtime/tp3/queries
