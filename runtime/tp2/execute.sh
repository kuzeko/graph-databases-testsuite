#!/bin/bash

# Prepare the query to be executed and execute it.
set -eu -o pipefail
IFS=$'\n\t'

# Compute paths
SCRIPTPATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"


[[ -z ${DEBUG+x} ]] || echo "Set WORKDIR to $SCRIPTPATH"
cd "$SCRIPTPATH"


# Extended path
export PATH=${EPATH:-}:$PATH

# Prepare the query
header.groovy.sh > /tmp/query

if [[ "$QUERY" == *loader.groovy ]]; then
  if [[ ! -f $DATASET ]]; then
    (>&2 echo "DATASET: '$DATASET' file does not exists.")
    exit 1
  fi

  # There is no boolean in bash
  if [[ -z ${NATIVE_LOADING+x}  ]]; then
    echo "Loading  with Gremlin"
    grep -v '^#' loader.groovy >> /tmp/query
  else
    echo "Native Loading already took place"
  fi

  grep -v '^#' sampler.groovy >> /tmp/query
elif [[ "$QUERY" == *create-index.groovy ]]  && ! [[ -z ${INDEX_QUERY+x}  ]]  ; then

  if [[ ! -f "$INDEX_QUERY" ]]; then
     (>&2 echo "QUERY: '$INDEX_QUERY' file does not exists.")
     exit 1
  fi
  echo "Use Native Indexing with $INDEX_QUERY"
  grep -v '^#' "$INDEX_QUERY" >> /tmp/query

else
  if [[ ! -f "queries/$QUERY" ]]; then
    (>&2 echo "QUERY: '$QUERY' file does not exists.")
    exit 1
  fi

  grep -v '^#' "queries/$QUERY" >> /tmp/query
fi

echo "System.exit(0)" >> /tmp/query

# Execute the query
LOG_T="$(date) $QUERY"
echo "$LOG_T" # to log.txt
echo "$LOG_T" >> "$RUNTIME_DIR/errors.log"

# NOTE: use the following invocation line for memory tracing
# /usr/bin/time -o '/runtime/memory.log' --append -f "$DATABASE,%t,%M" gremlin.sh -e /tmp/query 2>> "$RUNTIME_DIR/errors" | grep "^$DATABASE," >> /runtime/results

if [[ -z ${DEBUG+x} ]]; then
  # No debug mode
  echo "Grepping on $DATABASE,"
  if ! gremlin.sh -e /tmp/query 2>> "$RUNTIME_DIR/errors.log" | grep "^$DATABASE," >> "$RUNTIME_DIR/results.csv" ; then echo "grep end"; fi
  echo "DONE"
else
  echo "Running in DEBUG MODE $DEBUG"
  cat /tmp/query
  echo "$LOG_T" >> "$RUNTIME_DIR/results.csv"
  gremlin.sh -e /tmp/query 2>> "$RUNTIME_DIR/errors.log" 1>> "$RUNTIME_DIR/debug.log"
fi

unset DEBUG
