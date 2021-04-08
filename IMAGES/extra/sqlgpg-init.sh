#!/bin/bash

# maintainer: Brugnara

# This script is responsible for
#   1) spawing pg server
#   2) waiting for it be available
#   3) invoking the main shell with the proper arguments.

set -euo pipefail
IFS=$'\n\t'

# Init postgresql
set +u
# shellcheck disable=SC1090
source "$PWD/postgres-init.sh"
echo >&2 "[SQLG-INIT] PG - Initializing"
_custom_main >&2
echo >&2 "[SQLG-INIT] PG - Initalized"
set -u

# MB: inject custom config
custom_conf="/postgresql.conf"
if [ -f "$custom_conf" ]; then
  chown 999:999 "$custom_conf"
  mv "$custom_conf" /var/lib/postgresql/data/
fi

# Start postgresql
echo >&2 "[SQLG-INIT] PG - Starting"
PGUSER="${PGUSER:-$POSTGRES_USER}" \
  gosu postgres pg_ctl -D "$PGDATA" \
    -w start >&2
echo >&2 "[SQLG-INIT] PG - Started"

# Invoke the shell
echo >&2 "[SQLG-INIT] SHELL - invoke"
GDB_JAVA="java"
numa='numactl --interleave=all'
if $numa true &> /dev/null; then
  # shellcheck disable=SC2097,SC2098
  GDB_JAVA=$numa "$GDB_JAVA"
  echo >&2 '[INIT] Numa environment; numactl will be used.'
fi

DEFAULT_JAVA_OPTS="-XX:+UseG1GC"
IFS=$' \n\t'
$GDB_JAVA ${GDB_JVM_OPTS:-$DEFAULT_JAVA_OPTS} -jar /shell.jar "$@"
echo >&2 "[SQLG-INIT] SHELL - done"

if [[ -v GDB_SAFE_SHUTDOWN ]]; then
  echo >&2 '[SQLG-INIT] Shutting down the server.'
  PGUSER="${PGUSER:-$POSTGRES_USER}" \
    gosu postgres pg_ctl -D "$PGDATA" \
      -w stop >&2
  echo >&2 '[SQLG-INIT] Shut down completed.'
fi

exit 0
