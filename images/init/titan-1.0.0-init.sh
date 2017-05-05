#!/bin/bash
# Specific OrientDB dataset loader (via gremlin is too slow)
# maintainer: Lissandrini, Brugnara
set -euo pipefail
IFS=$'\n\t'

export NATIVE_LOADING=True

# THIS IS NOT EFFECTIVE BUT WE KEEP IT
export ADD_CP="${TITAN_HOME}/conf/log4j.properties"
if [[ -z ${CLASSPATH+x} ]]; then
  export CLASSPATH=$ADD_CP
else
  export CLASSPATH=${CLASSPATH}:$ADD_CP
fi

if ! [[ -z ${JAVA_OPTIONS+x} ]]; then
    echo "WARN:  JAVA_OPTIONS is set but this breaks Titan's version of gremlin console! Set TITAN_JAVA_OPTS instead!" >> ${RUNTIME_DIR}/errors
    TITAN_JAVA_OPTS=$JAVA_OPTIONS
    unset JAVA_OPTIONS
fi

if [[ "$QUERY" == *loader.groovy ]]; then

    echo "Loading log4j file" >> ${RUNTIME_DIR}/errors
    # THIS IS NOT EFFECTIVE BUT WE KEEP IT
    rm -v $TITAN_HOME/conf/log4j-console.properties     >> ${RUNTIME_DIR}/errors
    ln -s ${RUNTIME_DIR}/confs/titan-${TITAN_VERSION}-log4j-console.properties $TITAN_HOME/conf/log4j-console.properties
    ln -s ${RUNTIME_DIR}/confs/log4j-cassandra.properties $TITAN_HOME/conf/log4j.properties


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

    DNAME=$(basename ${DATASET})
    if [[  -f ${RUNTIME_DIR}/presampled/"${DNAME}_props.txt"  ]] && [[  -f ${RUNTIME_DIR}/presampled/"${DNAME}_labels.txt"  ]] ; then
         echo "Properties and labels already extracted!" >> ${RUNTIME_DIR}/errors
    else
         # We have to create the schema...
         echo "Extracting properties and labels" >> ${RUNTIME_DIR}/errors
         go run /extract_schema.go -i "${DATASET/%2/}" -l /labels.txt -p /props.txt
         echo "Finished Extraction of properties and labels" >> ${RUNTIME_DIR}/errors
    fi

    echo "Titan schema creation" >> ${RUNTIME_DIR}/errors
    gremlin.sh -e /titan-create-schema.groovy 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"

    rm -v /props.txt /labels.txt >> ${RUNTIME_DIR}/errors


    if [[ -z ${USE_INDEX+x} ]]; then
         echo "No Index" >> ${RUNTIME_DIR}/errors
    else
         echo "Creating Index..." >> ${RUNTIME_DIR}/errors
         start_time=$(date +%s)
         gremlin.sh -e /titan-create-index.groovy 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"
         end_time=$(date +%s)
         echo "Indexing done in " $(( ($end_time - $start_time) )) " secs" >> ${RUNTIME_DIR}/errors
    fi

    # DO LOADING
    ${RUNTIME_DIR}/tp3/header.groovy.sh > /tmp/loader.groovy
    grep -v '^#'  ${RUNTIME_DIR}/tp3/loader.groovy >> /tmp/loader.groovy
    echo "graph.close()" >> /tmp/loader.groovy
    echo "System.exit(0)" >> /tmp/loader.groovy

    cat /tmp/loader.groovy
    echo "Executing tp3 loader"  >> ${RUNTIME_DIR}/errors
    gremlin.sh -e /tmp/loader.groovy  2>> ${RUNTIME_DIR}/errors 1>> ${RUNTIME_DIR}/results



    # After loading remove the batch conf
    echo "Disable load opt"  >> ${RUNTIME_DIR}/errors
    sed -i 's/storage.batch-loading=true//g' "$TITAN_PROPERTIES"
    sed -i 's/schema.default=none//g' "$TITAN_PROPERTIES"
    tail -n 2 "$TITAN_PROPERTIES"

fi


. ${RUNTIME_DIR}/tp3/execute.sh


if [[ "$QUERY" == *loader.groovy ]]; then
    if [[ -z ${USE_INDEX+x} ]]; then
         echo "No Index" >> ${RUNTIME_DIR}/errors
    else
        echo "Dropping Index..." >> ${RUNTIME_DIR}/errors
        gremlin.sh -e /titan-drop-index.groovy 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"
        echo "Done!" >> ${RUNTIME_DIR}/errors
   fi
fi

