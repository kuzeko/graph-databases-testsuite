#!/bin/bash

# "Strict bash mode"
set -eu -o pipefail
IFS=$'\n\t'

# Compute paths
pushd "$(dirname "$0")" > /dev/null
SCRIPTPATH=$(pwd)
popd > /dev/null


# VARS
REPO="$SCRIPTPATH"
EPOCH=$(date +%s)

#-------------------------------------------------------------------------------
# Collect results
echo "Collect results"
mkdir -p "$REPO/collected/RESULTS"
# For each database in the results
RESULTS="$REPO/runtime/results.csv"
if [[ -f "$RESULTS" ]]; then
  echo "$RESULTS"
  if grep -c '^gremlin' "${RESULTS}" ; then
    grep '^gremlin' "$RESULTS" | awk -F',' '{print $1}' |\
      sort | uniq | while read -r DB
    do
      echo "Collecting results for $DB"
      grep "^$DB," "$RESULTS" > "$REPO/collected/RESULTS/${EPOCH}_${DB}_results.csv"
    done
  fi
fi

echo "Collect timeouts"
# For each database in the timeout
TIMEOUTS="$REPO/timeout.log"
if [[ -f "$TIMEOUTS" ]]; then
  awk -F',' '{print $2}' "$TIMEOUTS" | awk -F' ' '{print $NF}' |\
    sort | uniq | while read -r DB
  do
    echo "Collecting timeouts for $DB"
    TO_RES="$REPO/collected/RESULTS/${EPOCH}_${DB}_timeouts.csv"
    grep "$DB," "$TIMEOUTS" >> "$TO_RES"
  done
fi


#-------------------------------------------------------------------------------
# Collect raw files

echo "Collect raw"

RAW="$REPO/collected/RAW/${EPOCH}/"
mkdir -p "$RAW"

for fname in timeout docker test; do
[[ -f "$REPO"/${fname}.log ]] && mv -f "$REPO"/${fname}.log "$RAW"
done

for fname in $RESULTS errors.log debug.log; do
[[ -f "$REPO"/runtime/${fname} ]] && mv -f "$REPO"/runtime/${fname} "$RAW"
done


# This is needed to have the script end with a success status
echo "Done!"
