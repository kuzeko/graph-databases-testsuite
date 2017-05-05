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
echo "$LOG_T" >> "$RUNTIME_DIR/errors"

if [[ -z ${DEBUG+x} ]]; then
  # No debug mode
  echo "Grepping on $DATABASE,"
  if ! gremlin.sh -e /tmp/query 2>> "$RUNTIME_DIR/errors" | grep "^$DATABASE," >> /runtime/results ; then echo "grep end"; fi
  echo "DONE"
else
  echo "Running in DEBUG MODE $DEBUG"
  cat /tmp/query
  echo "$LOG_T" >> "$RUNTIME_DIR/results"
  gremlin.sh -e /tmp/query 2>> "$RUNTIME_DIR/errors" 1>> "$RUNTIME_DIR/results"
fi

unset DEBUG
