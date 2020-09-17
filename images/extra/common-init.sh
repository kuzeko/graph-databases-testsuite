#!/bin/bash

# maintainer: Brugnara

set -euo pipefail
IFS=$'\n\t'


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
$GDB_JAVA -jar /shell.jar "$@"
