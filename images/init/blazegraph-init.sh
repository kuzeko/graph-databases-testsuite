#!/bin/bash
# Specific Blazgraph bootstrap
# maintainer: Lissandrini
set -euo pipefail
IFS=$'\n\t'

if [[ -z ${JAVA_OPTS+x} ]]; then

   echo "NO JAVA_OPTS SET TO BLAZEGRAPH - SETTING DEFAULT"
   export JAVA_OPTS=-Xmx8g

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

    echo "Loading Blazegraph Configuration $DATASET" >> ${RUNTIME_DIR}/errors
    gremlin.sh -e  /tmp/blazegraph-config.groovy   2>> ${RUNTIME_DIR}/errors 1>> ${RUNTIME_DIR}/results
    echo "Loading Configuration Done!"  >> ${RUNTIME_DIR}/errors

    ${RUNTIME_DIR}/tp3/header.groovy.sh > /tmp/loader.groovy
    sed 's/#BULK//g'  ${RUNTIME_DIR}/tp3/loader.groovy |  grep -v '^#' >>  /tmp/loader.groovy
    #echo "graph.close()" >> /tmp/loader.groovy
    echo 'System.err.println("Close")' >> /tmp/loader.groovy
    echo "System.exit(0)" >> /tmp/loader.groovy

    cat /tmp/loader.groovy

    gremlin.sh -e /tmp/loader.groovy  2>> ${RUNTIME_DIR}/errors 1>> ${RUNTIME_DIR}/results
fi


. ${RUNTIME_DIR}/tp3/execute.sh
