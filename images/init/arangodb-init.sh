#!/bin/bash
# Specific ArangoDB dataset loader (via gremlin is too slow)
# maintainer: Brugnara
set -euo pipefail
IFS=$'\n\t'

# Additional operations required by some graph databases engine
status_ok() {
  # Wait until given url return HTTP 200 OK
  # Used to wait for DB to wake up.

  # It's OK if it fails if web server is down.
  set +e
  local STARTTIME=$(date +%s)
  local C=1
  local URL=$1
  while true; do
    status=$(curl -s -o /dev/null -w "%{http_code}\n" "${URL}")
    [[ "$?" -eq 0 ]] && [[ "$status" -eq 200 ]] && break
    echo "[${status}] Waiting for 200 OK @ ${URL}"
    C=$((C+1))
    sleep $((C + C/2))
  done
  local ENDTIME=$(date +%s)
  echo "Arango up in $(($ENDTIME - $STARTTIME)) seconds"

  set -e
}


if [[ -z ${CLASSPATH+x} ]]; then

   # Load logger config
   echo "NO CLASSPATH SET TO ARANGO - SETTING DEFAULT"
   export CLASSPATH=${RUNTIME_DIR}/confs

fi

echo $CLASSPATH


#if [ -d "/var/lib/arangodb/databases" ]; then
# date >> /runtime/logs/arango.databases.log
# ls -alR /var/lib/arangodb/databases >> /runtime/logs/arango.databases.log
#fi

# Clean up and prepare dirs for pid, locks, and db.

# Be sure we can read and log to presisten directories
chmod 777 ${RUNTIME_DIR}/logs
chmod 755 ${RUNTIME_DIR}/confs

# Be sure we can create pid file
mkdir -p /var/run/arangodb/
chown arangodb:arangodb /var/run/arangodb
chmod 755 /var/run/arangodb/

# Delete loading leftovers (pid and locks)
ps fax | grep arangod
echo "***RUN"
ls -v /var/run/arangodb/
rm -f /var/run/arangodb/arangodb.pid
echo "***VAR"
ls /var/lib/arangodb/
rm -f /var/lib/arangodb/LOCK
#arangod --server.disable-authentication="true" --server.disable-statistics true --daemon --pid-file /etc/arangodb/arangodb.pid -c /runtime/arangod.conf
arangod --daemon \
	--pid-file /var/run/arangodb/arangodb.pid \
	-c ${RUNTIME_DIR}/confs/arangod.conf
echo 'Waiting for arangodb on localhost...'

status_ok 'http://localhost:8529/_db/_system/_admin/aardvark/standalone.html'


export NATIVE_LOADING=True

if [[ "$QUERY" == *loader.groovy ]]
then
  # $DATASET into Split nodes.json and edges.json
  # 1 entry per row
  echo "Converting JSON"
  go run /main.go -i "${DATASET/%2/}"

  echo "Loading nodes"
  start_time=$(($(date +%s%N)/1000000))
  # Load nodes
  arangoimp --server.endpoint tcp://localhost:8529 --file "nodes.json" --type json --collection "V" --batch-size 10240 --create-collection true
  end_time=$(($(date +%s%N)/1000000))
  echo  ${DATABASE},${DATASET},${QUERY},,,,$(( ($end_time - $start_time) )),native,nodes | tee -a ${RUNTIME_DIR}/results

  # Create edges collection
  echo "Create edges collection"
  curl -X POST --data-binary @- --dump - http://localhost:8529/_api/collection <<EOF
  {
    "name" : "E",
    "type" : 3
  }
EOF

  # Load edges
  echo "Loading edges"
  start_time=$(($(date +%s%N)/1000000))
  arangoimp --server.endpoint tcp://localhost:8529 --file "edges.json" --type json --collection "E" --batch-size 10240
  end_time=$(($(date +%s%N)/1000000))
  echo  ${DATABASE},${DATASET},${QUERY},,,,$(( ($end_time - $start_time) )),native,edges | tee -a ${RUNTIME_DIR}/results

  rm -vf ${RUNTIME_DIR}/nodes.json ${RUNTIME_DIR}/edges.json
  echo "Done"
fi

  curl -s "http://localhost:8529/_api/collection/V/properties"
  curl -s "http://localhost:8529/_api/collection/E/properties"

  . ${RUNTIME_DIR}/tp2/execute.sh

