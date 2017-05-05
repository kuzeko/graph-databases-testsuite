#!/bin/bash
# Specific Blazgraph bootstrap
# maintainer: Lissandrini
set -euo pipefail
IFS=$'\n\t'


if [[ "$QUERY" == *loader.groovy ]]; then
    if [[ "$DATASET" == *.json2 ]]; then
        echo "Required conversion for a json2 file $DATASET ."
        DATASET="${DATASET/%2/}"
        echo "Let's try to produce it from $DATASET ."
    fi

    if [[ "$DATASET" != *.json2 && ! -f "${DATASET}2" ]]; then
        echo "CONVERT $DATASET to Tinkerpop3 Compatible" >> ${RUNTIME_DIR}/errors
        # Uses Neo4j for the conversion
        echo 'def DB_FILE="/srv/db"; def conf = ["node_cache_size":"100M","relationship_cache_size":"100M"]; g=new Neo4jGraph(DB_FILE, conf)'  > /tmp/loader.groovy
        grep -v '^#'  ${RUNTIME_DIR}/converter.groovy  >> /tmp/loader.groovy
        echo 'System.err.println("Gremlin Done"); System.exit(0);'  >> /tmp/loader.groovy

        /opt/gremlin2/bin/gremlin2.sh -e  /tmp/loader.groovy   2>> ${RUNTIME_DIR}/errors 1>> ${RUNTIME_DIR}/results
        DATASET="${DATASET}2"
        echo "CONVERTED $DATASET to Tinkerpop3 " >> /runtime/errors
    elif [[ "$DATASET" != *.json2 && -f "${DATASET}2" ]]; then
        echo "${DATASET}2 ALREADY PRESENT" >> /runtime/errors
        DATASET="${DATASET}2"
    else
        echo "$DATASET ALREADY tinkerpop3 Compatible" >> ${RUNTIME_DIR}/errors
    fi

else
    echo "Aye! Only Loading!" >> ${RUNTIME_DIR}/errors
fi



