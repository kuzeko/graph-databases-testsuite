#!/bin/bash
# maintainer: Brugnara

# Start cassandra and wait
echo >&2 '[JANUS-INIT] Starting cassandra'
source /cassandra.sh >&2

# Start elastic and wait
echo >&2 '[JANUS-INIT] Starting elasticsearch'
source /elastic.sh >&2

# set -euo pipefail
IFS=$'\n\t'

echo >&2 'Invoking the shell'
GDB_JAVA="java"
numa='numactl --interleave=all'
if $numa true &> /dev/null; then
  # shellcheck disable=SC2097,SC2098
  GDB_JAVA=$numa "$GDB_JAVA"
  echo >&2 '[INIT] Numa environment; numactl will be used.'
fi

DEFAULT_JAVA_OPTS="-XX:+UseG1GC"
GDB_JAVA="$GDB_JAVA ${GDB_JVM_OPTS:-$DEFAULT_JAVA_OPTS}"
IFS=$' \n\t'
$GDB_JAVA -Dlogback.configurationFile=/logback.xml -jar /shell.jar "$@"

if [[ -v GDB_SAFE_SHUTDOWN ]]; then
  echo >&2 '[JANUS-INIT] Shutting down the server.'

  echo >&2 '[JANUS-INIT] Killing Cassandra.'
  cpid=$(cat /tmp/gdb_cass.pid)
  kill -s TERM "$cpid"
  while [ -e "/proc/$cpid" ]; do
    echo >&2 '[JANUS-INIT] Waiting for Cassandra to die.'
    sleep .6
  done

  epid=$(cat /tmp/gdb_elastic.pid)
  kill -s TERM "$epid"
  while [ -e "/proc/$epid" ]; do
    echo >&2 '[JANUS-INIT] Waiting for Elastic to die.'
    sleep .6
  done

  # FIXME: the following unset does not work...
  #        is the docker image keeping track of '-e'?
  unset GDB_SAFE_SHUTDOWN   # Avoid commiting this flag
  echo  >&2 '[JANUS-INIT] Safe shutdown completed.'
  exit 0
fi

exit 0
