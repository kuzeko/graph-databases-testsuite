#!/bin/bash
# Specific Neo4J TP-3 bootstrap
# maintainer: Lissandrini
set -euo pipefail
IFS=$'\n\t'

if [[ -z ${JAVA_OPTS+x} ]]; then

   echo "NO JAVA_OPTS SET TO NEO4J TP3 - SETTING DEFAULT"
   export JAVA_OPTS='-Xms4g -Xmn128M -Xmx120g'

fi

echo "JAVA_OPTS=$JAVA_OPTS"

export NATIVE_LOADING=True

if [[ "$QUERY" == *loader.groovy ]]; then

    echo "Loading $DATASET" >> ${RUNTIME_DIR}/errors
    if [[ "$DATASET" = *.json3 ]]; then
        echo "ALREADY tinkerpop3 NEW V. $DATASET" >> ${RUNTIME_DIR}/errors
    elif [[ "$DATASET" != *.json2 && ! -f "${DATASET}2" ]]; then
        echo "NOT tinkerpop3 $DATASET!" >> ${RUNTIME_DIR}/errors
        exit 2
    elif [[ "$DATASET" != *.json2 && -f "${DATASET}2" ]]; then
        echo "USE tinkerpop3 ${DATASET}2" >> ${RUNTIME_DIR}/errors
        DATASET="${DATASET}2"
    else
        echo "ALREADY tinkerpop3 $DATASET" >> ${RUNTIME_DIR}/errors
    fi

    ${RUNTIME_DIR}/tp3/header.groovy.sh > /tmp/loader.groovy
    grep -v '^#'  ${RUNTIME_DIR}/tp3/loader.groovy >> /tmp/loader.groovy
    echo "graph.close()" >> /tmp/loader.groovy
    echo "System.exit(0)" >> /tmp/loader.groovy

    cat /tmp/loader.groovy
    echo "Executing tp3 loader"
    gremlin.sh -e /tmp/loader.groovy  2>> ${RUNTIME_DIR}/errors 1>> ${RUNTIME_DIR}/results
fi


. ${RUNTIME_DIR}/tp3/execute.sh
