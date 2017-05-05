#!/bin/bash
# Specific OrientDB dataset loader (via gremlin is too slow)
# maintainer: Lissandrini, Brugnara
set -euo pipefail
IFS=$'\n\t'



if [[ -z ${ORIENTDB_OPTS_MEMORY+x} ]]; then
  echo "NO ORIENTDB_OPTS_MEMORY"
else
  echo "ORIENTDB_OPTS_MEMORY=${ORIENTDB_OPTS_MEMORY}"
fi

if [[ -z ${JAVA_OPTIONS+x} ]]; then
  echo "NO JAVA_OPTIONS"
else
  echo "JAVA_OPTIONS=${JAVA_OPTIONS}"
fi


if [[ -z ${JAVA_OPTS+x} ]]; then
  echo "NO JAVA_OPTS"
else
  echo "JAVA_OPTS=${JAVA_OPTS}"
fi

# NOTE: now (2.2.13) it should just work with gremlin
# The following use native loader

export NATIVE_LOADING=True

if [[ "$QUERY" == *loader.groovy ]]; then
    # TODO: refactor, gen outise $RUNTIME_DIR, rm after run

    echo "OrientDB (removeing slash for) loading $DATASET" | tee -a ${RUNTIME_DIR}/errors
    DATASET_NAME=$(basename "${DATASET}")
    SAFE_DATASET="${DATASET_NAME}_noslash"
    # sed '/\// s//__/g' "$DATASET"  > "$SAFE_DATASET"
    perl -pe "s/\//__/g" < "$DATASET" > "$SAFE_DATASET"
    echo "Created $SAFE_DATASET" | tee -a ${RUNTIME_DIR}/errors

    # database path (/srv/db) must be the same as DB_FILE in $RUNTIME_DIR/tp2/header.groovy.sh
    if [[ -z ${MINIMUMCLUSTERS+x} ]]; then
      echo "CREATE DATABASE PLOCAL:/srv/db ;ALTER DATABASE minimumclusters 1 ;QUIT" | "$ORIENTDB_HOME"/bin/console.sh >> ${RUNTIME_DIR}/errors
    else
      echo "CREATE DATABASE PLOCAL:/srv/db ;QUIT" | "$ORIENTDB_HOME"/bin/console.sh >> ${RUNTIME_DIR}/errors
    fi

    start_time=$(($(date +%s%N)/1000000))
    echo "CONNECT PLOCAL:/srv/db admin admin ;IMPORT DATABASE ${SAFE_DATASET} -format=graphson ;QUIT" | time "$ORIENTDB_HOME"/bin/console.sh
    end_time=$(($(date +%s%N)/1000000))
    echo  "${DATABASE},${DATASET},${QUERY},,,,$(( (end_time - start_time) )),native" | tee -a ${RUNTIME_DIR}/results

    echo "Done loading" >> ${RUNTIME_DIR}/errors
    rm -v "${SAFE_DATASET}"
fi

. ${RUNTIME_DIR}/tp2/execute.sh

echo 'Done'
