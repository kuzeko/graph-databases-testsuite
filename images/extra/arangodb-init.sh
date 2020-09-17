#!/bin/bash

# maintainer: Brugnara

# This script is responsible for
#   1) spawing aragnodb server
#   2) waiting for it be available
#   3) invoking the main shell with the proper arguments.

# This script is derived from the official docker-entrypont.sh

set -euo pipefail
IFS=$'\n\t'


export GLIBCXX_FORCE_NEW=1    # Dunno what this does.


# Note:
# /var/lib/arangodb3 and /var/lib/arangodb3-apps must exist and
# be writable by the user under which we run the container.

# Make a copy of the configuration file to patch it, note that this
# must work regardless under which user we run:
cp /etc/arangodb3/arangod.conf /tmp/arangod.conf
cp /etc/arangodb3/arango-init-database.conf /tmp/arango-init-database.conf

# Set storage engine.
# https://www.arangodb.com/docs/stable/architecture-storage-engines.html
ARANGO_STORAGE_ENGINE="rocksdb" # Alternatives: rocksdb, mmfiles
sed -i /tmp/arangod.conf -e "s;storage-engine = auto;storage-engine = $ARANGO_STORAGE_ENGINE;"


# Wait for ArangoDB to come online or die (max 120s).
wait_4db() {
  local TIMEOUT
  TIMEOUT=${1:-120}

  local STARTTIME
  STARTTIME=$(date +%s)

  local counter=0
  ARANGO_UP=0

  while [ "$ARANGO_UP" = "0" ]; do
    if [ $counter -gt 0 ]; then
      sleep 1
    fi

    if [ "$counter" -gt "$TIMEOUT" ]; then
      echo >&2  "[ARANGODB-INIT] ArangoDB didn't start correctly during init"
      cat /tmp/init-log
      exit 1
    fi

    counter=$(( counter + 1 ))

    ARANGO_UP=1
    # Check if it is online now
    arangosh \
        --server.endpoint=unix:///tmp/arangodb-tmp.sock \
        --server.authentication false \
        --javascript.execute-string "db._version()" \
        > /dev/null 2>&1 || ARANGO_UP=0
  done

  local ENDTIME
  ENDTIME=$(date +%s)

  echo >&2  "[ARANGODB-INIT] Arango up in $((ENDTIME - STARTTIME)) seconds"
}


# Database initialization
if [ ! -f /var/lib/arangodb3/SERVER ]; then
  echo >&2  "[ARANGODB-INIT] Initializing database...Hang on..."
  arangod --config /tmp/arangod.conf \
          --server.endpoint unix:///tmp/arangodb-tmp.sock \
          --server.authentication false \
          --log.file /tmp/init-log \
          --log.foreground-tty false &
  pid="$!"

  # Wait for the db to come online
  wait_4db 100

  # Kill the DB
  if ! kill -s TERM "$pid" || ! wait "$pid"; then
    echo >&2 '[ARANGODB-INIT] ArangoDB Init failed.'
    exit 1
  fi

  echo >&2  "[ARANGODB-INIT] Database initialized...Starting System..."
else
  echo >&2  "[ARANGODB-INIT] starting over with existing database"
fi

# Two endpoints: unix, for the shell, tcp for gremlin.
START_ARANGO="arangod --server.authentication=false --server.endpoint unix:///tmp/arangodb-tmp.sock --server.endpoint tcp://127.0.0.1:8529 --config /tmp/arangod.conf"

# Always try to set NUMA policy, worst case do nothing.
# https://stackoverflow.com/questions/33315950/mongodb-in-docker-numactl-interleave-all-explanation
echo >&2  "[ARANGODB-INIT] Will try to enable NUMA interleave on all nodes..."

GDB_JAVA="java"
# Check numactl usability to provide a hint if container is not running in privileged mode
numa='numactl --interleave=all'
if $numa true &> /dev/null; then
  # shellcheck disable=SC2097,SC2098
  START_ARANGO=$numa "$START_ARANGO"
  # shellcheck disable=SC2097,SC2098
  GDB_JAVA=$numa "$GDB_JAVA"
else
  echo >&2 "[ARANGODB-INIT] error: cannot use numactl, if you are sure that it is supported by your hardware and system, please check that container is running with --security-opt seccomp=unconfined"
fi

# Start the server with authentication disabled
bash -c "$START_ARANGO" > /dev/null 2>&1 &
pid="$!"

# Wait for the database to come online (max 120s)
wait_4db 120

DEFAULT_JAVA_OPTS="-XX:+UseG1GC"
GDB_JAVA="$GDB_JAVA ${GDB_JVM_OPTS:-$DEFAULT_JAVA_OPTS}"
IFS=$' \n\t'
$GDB_JAVA -jar /shell.jar "$@"

if [[ -v GDB_SAFE_SHUTDOWN ]]; then
  echo >&2 '[ARANGODB-INIT] Shutting down ArangoDB server.'

  if ! kill -s TERM "$pid" || ! wait "$pid"; then
    echo >&2 '[ARANGODB-INIT] ArangoDB safe shutdown failed.'
    exit 1
  fi
  # FIXME: the following unset does not work...
  #        is the docker image keeping track of '-e'?
  unset GDB_SAFE_SHUTDOWN   # Avoid commiting this flag
  echo  >&2 '[ARANGODB-INIT] ArangoDB safe shutdown completed.'
  exit 0
fi

exit 0
