#!/bin/bash
# Specific OrientDB dataset loader (via gremlin is too slow)
# maintainer: Lissandrini, Brugnara
set -euo pipefail
IFS=$'\n\t'

if ! [[ -z ${JAVA_OPTIONS+x} ]]; then
    echo "WARN:  JAVA_OPTIONS is set but this breaks Titan's version of gremlin console! Set TITAN_JAVA_OPTS instead!" >> ${RUNTIME_DIR}/errors
    TITAN_JAVA_OPTS=$JAVA_OPTIONS
    unset JAVA_OPTIONS
fi

if [[ "$QUERY" == *loader.groovy ]]; then

    DNAME=$(basename ${DATASET})
    if [[  -f ${RUNTIME_DIR}/presampled/"${DNAME}_props.txt"  ]] && [[  -f ${RUNTIME_DIR}/presampled/"${DNAME}_labels.txt"  ]] ; then
         echo "Properties and labels already extracted!" >> ${RUNTIME_DIR}/errors
    else
         # We have to create the schema...
         echo "Extracting properties and labels" >> ${RUNTIME_DIR}/errors
         go run /extract_schema.go -i "${DATASET/%2/}" -l /labels.txt -p /props.txt
    fi

    echo "Titan schema creation"  >> ${RUNTIME_DIR}/errors
    gremlin.sh -e /titan-create-schema.groovy 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"

    rm -v /props.txt /labels.txt >> ${RUNTIME_DIR}/errors

    if [[ -z ${USE_INDEX+x} ]]; then
         echo "No Index" >> ${RUNTIME_DIR}/errors
    else
         cd /tmp
         echo "Creating Index..." >> ${RUNTIME_DIR}/errors
         start_time=$(date +%s)
         gremlin.sh -e /titan-create-index.groovy 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"
         # Reindex doesn't work
         #gremlin.sh -e /titan-re-index.groovy 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"
         end_time=$(date +%s)
         echo "Indexing done in " $(( ($end_time - $start_time) )) " secs" >> "${RUNTIME_DIR}/errors"
         cd $RUNTIME_DIR
    fi


    # DO LOADING
    echo "Loading" >> ${RUNTIME_DIR}/errors
    $RUNTIME_DIR/tp2/header.groovy.sh > /tmp/loader.groovy
    grep -v '^#' $RUNTIME_DIR/tp2/loader.groovy >> /tmp/loader.groovy
    printf "\ng.shutdown()\n" >> /tmp/loader.groovy
    printf "\nSystem.exit(0)\n" >> /tmp/loader.groovy

    gremlin.sh -e /tmp/loader.groovy 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"


    # After loading remove the batch conf
    echo "Disable load opt" >> ${RUNTIME_DIR}/errors
    sed -i 's/storage.batch-loading=true//g' "$TITAN_PROPERTIES"
    sed -i 's/schema.default=none//g' "$TITAN_PROPERTIES"
    tail -n 2 "$TITAN_PROPERTIES" >> ${RUNTIME_DIR}/errors
fi

export NATIVE_LOADING=1

. $RUNTIME_DIR/tp2/execute.sh

if [[ "$QUERY" == *loader.groovy ]]; then

    if [[ -z ${USE_INDEX+x} ]]; then
         echo "No Index" >> ${RUNTIME_DIR}/errors
    else
         cd /tmp
         echo "Dropping Index..." >> ${RUNTIME_DIR}/errors
         gremlin.sh -e /titan-drop-index.groovy 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"
         echo "Done!" >> ${RUNTIME_DIR}/errors
         cd $RUNTIME_DIR
    fi
fi
