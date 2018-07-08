#!/bin/bash
# Specific OrientDB dataset loader (via gremlin is too slow)
# maintainer: Lissandrini, Brugnara
set -euo pipefail
IFS=$'\n\t'

export NATIVE_LOADING=1
#export INDEX_QUERY="/titan-create-index.groovy"

if ! [[ -z ${JAVA_OPTIONS+x} ]]; then
    echo "WARN:  JAVA_OPTIONS is set but this breaks Titan's version of gremlin console! Set TITAN_JAVA_OPTS instead!" >> ${RUNTIME_DIR}/errors.log
    TITAN_JAVA_OPTS=$JAVA_OPTIONS
    unset JAVA_OPTIONS
fi

if [[ "$QUERY" == *loader.groovy ]]; then

    DNAME=$(basename ${DATASET})
    if [[  -f ${RUNTIME_DIR}/presampled/"${DNAME}_props.txt"  ]] && [[  -f ${RUNTIME_DIR}/presampled/"${DNAME}_labels.txt"  ]] ; then
         echo "Properties and labels already extracted!" >> ${RUNTIME_DIR}/errors.log
    else
         # We have to create the schema...
         echo "Extracting properties and labels" >> ${RUNTIME_DIR}/errors.log
         go run /extract_schema.go -i "${DATASET/%2/}" -l /labels.txt -p /props.txt
    fi

    echo "Titan schema creation"  >> ${RUNTIME_DIR}/errors.log
    gremlin.sh -e /titan-create-schema.groovy 2>> "$RUNTIME_DIR/errors.log" 1>> "$RUNTIME_DIR/results.csv"

    rm -v /props.txt /labels.txt >> ${RUNTIME_DIR}/errors.log

    if [[ -z ${USE_INDEX+x} ]]; then
         echo "No Index" >> ${RUNTIME_DIR}/errors.log
    else
         cd /tmp
         $RUNTIME_DIR/tp2/header.groovy.sh > /tmp/index.groovy
         grep -v '^#' /titan-create-index.groovy >> /tmp/index.groovy
         echo "graph.close()" >> /tmp/index.groovy
         echo "System.exit(0)" >> /tmp/index.groovy
         cat /tmp/index.groovy

         echo "Creating Index..." >> ${RUNTIME_DIR}/errors.log
         start_time=$(date +%s)
         gremlin.sh -e /tmp/index.groovy 2>> "$RUNTIME_DIR/errors.log" 1>> "$RUNTIME_DIR/results.csv"
         end_time=$(date +%s)
         echo "Indexing done in " $(( ($end_time - $start_time) )) " secs" >> "${RUNTIME_DIR}/errors.log"
         cd $RUNTIME_DIR
    fi


    # DO LOADING
    echo "Loading" >> ${RUNTIME_DIR}/errors.log
    $RUNTIME_DIR/tp2/header.groovy.sh > /tmp/loader.groovy
    grep -v '^#' $RUNTIME_DIR/tp2/loader.groovy >> /tmp/loader.groovy
    printf "\ng.shutdown()\n" >> /tmp/loader.groovy
    printf "\nSystem.exit(0)\n" >> /tmp/loader.groovy

    gremlin.sh -e /tmp/loader.groovy 2>> "$RUNTIME_DIR/errors.log" 1>> "$RUNTIME_DIR/results.csv"


    # After loading remove the batch conf
    echo "Disable load opt" >> ${RUNTIME_DIR}/errors.log
    sed -i 's/storage.batch-loading=true//g' "$TITAN_PROPERTIES"
    sed -i 's/schema.default=none//g' "$TITAN_PROPERTIES"
    tail -n 2 "$TITAN_PROPERTIES" >> ${RUNTIME_DIR}/errors.log
fi


. $RUNTIME_DIR/tp2/execute.sh

if [[ "$QUERY" == *loader.groovy ]]; then

    if [[ -z ${USE_INDEX+x} ]]; then
         echo "No Index" >> ${RUNTIME_DIR}/errors.log
    else
         cd /tmp
         echo "Dropping Index..." >> ${RUNTIME_DIR}/errors.log
         gremlin.sh -e /titan-drop-index.groovy 2>> "$RUNTIME_DIR/errors.log" 1>> "$RUNTIME_DIR/results.csv"
         echo "Done!" >> ${RUNTIME_DIR}/errors.log
         cd $RUNTIME_DIR
    fi
fi
