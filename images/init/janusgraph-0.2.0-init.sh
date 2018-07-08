#!/bin/bash
set -euo pipefail
IFS=$'\n\t'

export NATIVE_LOADING=True
#export INDEX_QUERY="/janusgraph-create-index.groovy"

# THIS IS NOT EFFECTIVE BUT WE KEEP IT
export ADD_CP="${JANUS_HOME}/conf/log4j.properties"
if [[ -z ${CLASSPATH+x} ]]; then
  export CLASSPATH=$ADD_CP
else
  export CLASSPATH=${CLASSPATH}:$ADD_CP
fi

if ! [[ -z ${JAVA_OPTIONS+x} ]]; then
    echo "WARN:  JAVA_OPTIONS is set but this breaks Janus's version of gremlin console! Set JANUS_JAVA_OPTS instead!" >> ${RUNTIME_DIR}/errors.log
    JANUS_JAVA_OPTS=$JAVA_OPTIONS
    unset JAVA_OPTIONS
fi

if [[ -z ${JANUS_JAVA_OPTS+x} ]]; then
    echo "WARN:  JANUS_JAVA_OPTS is  NOT set. We'll get some defaults!" >> ${RUNTIME_DIR}/errors.log
    export JANUS_JAVA_OPTS='-Xms1G -Xmn128M -Xmx8G'
fi

if [[ "$QUERY" == *loader.groovy ]]; then

    echo "Loading log4j file" >> ${RUNTIME_DIR}/errors.log
    # THIS IS NOT EFFECTIVE BUT WE KEEP IT
    rm -v $JANUS_HOME/conf/log4j-console.properties     >> ${RUNTIME_DIR}/errors.log
    ln -s ${RUNTIME_DIR}/confs/janusgraph-${JANUS_VERSION}-log4j-console.properties $JANUS_HOME/conf/log4j-console.properties
    ln -s ${RUNTIME_DIR}/confs/log4j-cassandra.properties $JANUS_HOME/conf/log4j.properties


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

    DNAME=$(basename ${DATASET})
    if [[  -f ${RUNTIME_DIR}/presampled/"${DNAME}_props.txt"  ]] && [[  -f ${RUNTIME_DIR}/presampled/"${DNAME}_labels.txt"  ]] ; then
         echo "Properties and labels already extracted!" >> ${RUNTIME_DIR}/errors.log
    else
         # We have to create the schema...
         echo "Extracting properties and labels ${DATASET/%2/}" >> ${RUNTIME_DIR}/errors.log
         echo "Extracting properties and labels ${DATASET/%2/}" >> ${RUNTIME_DIR}/errors.log
         go run /extract_schema.go -i "${DATASET/%2/}" -l /labels.txt -p /props.txt >> ${RUNTIME_DIR}/errors.log
         echo "Finished Extraction of properties and labels" >> ${RUNTIME_DIR}/errors.log
    fi

    echo "JanusGraph schema creation" >> ${RUNTIME_DIR}/errors.log
    gremlin.sh -e /janusgraph-create-schema.groovy 2>> "$RUNTIME_DIR/errors.log" 1>> "$RUNTIME_DIR/results.csv"

    rm -v /props.txt /labels.txt >> ${RUNTIME_DIR}/errors.log


    if [[ -z ${USE_INDEX+x} ]]; then
         echo "No Index" >> ${RUNTIME_DIR}/errors.log
    else
         ${RUNTIME_DIR}/tp3/header.groovy.sh > /tmp/index.groovy
         grep -v '^#' /janusgraph-create-index.groovy >> /tmp/index.groovy
         echo "graph.close()" >> /tmp/index.groovy
         echo "System.exit(0)" >> /tmp/index.groovy
         cat /tmp/index.groovy

         echo "Creating Index..." >> ${RUNTIME_DIR}/errors.log
         start_time=$(date +%s)
         gremlin.sh -e /tmp/index.groovy 2>> "$RUNTIME_DIR/errors.log" 1>> "$RUNTIME_DIR/results.csv"
         end_time=$(date +%s)
         echo "Indexing done in " $(( ($end_time - $start_time) )) " secs" >> ${RUNTIME_DIR}/errors.log
    fi

    # DO LOADING
    ${RUNTIME_DIR}/tp3/header.groovy.sh > /tmp/loader.groovy
    grep -v '^#'  ${RUNTIME_DIR}/tp3/loader.groovy >> /tmp/loader.groovy
    echo "graph.close()" >> /tmp/loader.groovy
    echo "System.exit(0)" >> /tmp/loader.groovy

    cat /tmp/loader.groovy
    echo "Executing tp3 loader"  >> ${RUNTIME_DIR}/errors.log
    gremlin.sh -e /tmp/loader.groovy  2>> ${RUNTIME_DIR}/errors.log 1>> ${RUNTIME_DIR}/results.csv



    # After loading remove the batch conf
    echo "Disable load opt"  >> ${RUNTIME_DIR}/errors.log
    sed -i 's/storage.batch-loading=true//g' "$JANUS_PROPERTIES"
    sed -i 's/schema.default=none//g' "$JANUS_PROPERTIES"
    tail -n 2 "$JANUS_PROPERTIES"

fi



. ${RUNTIME_DIR}/tp3/execute.sh


if [[ "$QUERY" == *loader.groovy ]]; then
    if [[ -z ${USE_INDEX+x} ]]; then
         echo "No Index" >> ${RUNTIME_DIR}/errors.log
    else
        echo "Dropping Index..." >> ${RUNTIME_DIR}/errors.log
        gremlin.sh -e /janusgraph-drop-index.groovy 2>> "$RUNTIME_DIR/errors.log" 1>> "$RUNTIME_DIR/results.csv"
        echo "Done!" >> ${RUNTIME_DIR}/errors.log
   fi
fi



