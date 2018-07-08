#!/bin/bash
# Specific Neo4J TP-3 bootstrap
# maintainer: Lissandrini
set -euo pipefail
IFS=$'\n\t'

if [[ -z ${JAVA_OPTIONS+x} ]]; then

   echo "WARN: NO JAVA_OPTIONS SET TO NEO4J TP3 - SETTING DEFAULT"
   export JAVA_OPTIONS='-Xms4g -Xmn128M -Xmx120g'

fi

echo "JAVA_OPTIONS=$JAVA_OPTIONS"

export NATIVE_LOADING=True

if [[ "$QUERY" == *loader.groovy ]]; then

    echo "Loading $DATASET" >> ${RUNTIME_DIR}/errors.log
    if [[ "$DATASET" = *.json3 ]]; then
        echo "ALREADY tinkerpop3 NEW V. $DATASET" >> ${RUNTIME_DIR}/errors.log
    elif [[ "$DATASET" != *.json2 && ! -f "${DATASET}2" ]]; then
        echo "NOT tinkerpop3 $DATASET!" >> ${RUNTIME_DIR}/errors.log
        exit 2
    elif [[ "$DATASET" != *.json2 && -f "${DATASET}2" ]]; then
        echo "USE tinkerpop3 ${DATASET}2" >> ${RUNTIME_DIR}/errors.log
        DATASET="${DATASET}2"
    else
        echo "ALREADY tinkerpop3 $DATASET" >> ${RUNTIME_DIR}/errors.log
    fi

    ${RUNTIME_DIR}/tp3/header.groovy.sh > /tmp/loader.groovy
    grep -v '^#'  ${RUNTIME_DIR}/tp3/loader.groovy >> /tmp/loader.groovy
    echo "graph.close()" >> /tmp/loader.groovy
    echo "System.exit(0)" >> /tmp/loader.groovy

    cat /tmp/loader.groovy
    echo "Executing tp3 loader"
    gremlin.sh -e /tmp/loader.groovy  2>> ${RUNTIME_DIR}/errors.log 1>> ${RUNTIME_DIR}/results.csv
fi


. ${RUNTIME_DIR}/tp3/execute.sh
