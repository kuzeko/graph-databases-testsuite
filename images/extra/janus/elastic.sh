#!/bin/bash

# Files created by Elasticsearch should always be group writable too
umask 0002

run_as_other_user_if_needed() {
  if [[ "$(id -u)" == "0" ]]; then
    # If running as root, drop to specified UID and run command
    chroot --userspec=1000 / "${@}"
    return
  else
    # Either we are running in Openshift with random uid and are a member of the root group
    # or with a custom --user
    "${@}"
    return
  fi
}

# Allow environment variables to be set by creating a file with the
# contents, and setting an environment variable with the suffix _FILE to
# point to it. This can be used to provide secrets to a container, without
# the values being specified explicitly when running the container.
#
# This is also sourced in elasticsearch-env, and is only needed here
# as well because we use ELASTIC_PASSWORD below. Sourcing this script
# is idempotent.
#source /usr/share/elasticsearch/bin/elasticsearch-env-from-file

if [[ -f bin/elasticsearch-users ]]; then
  # Check for the ELASTIC_PASSWORD environment variable to set the
  # bootstrap password for Security.
  #
  # This is only required for the first node in a cluster with Security
  # enabled, but we have no way of knowing which node we are yet. We'll just
  # honor the variable if it's present.
  if [[ -n "$ELASTIC_PASSWORD" ]]; then
    [[ -f /usr/share/elasticsearch/config/elasticsearch.keystore ]] || (run_as_other_user_if_needed elasticsearch-keystore create)
    if ! (run_as_other_user_if_needed elasticsearch-keystore list | grep -q '^bootstrap.password$'); then
      (run_as_other_user_if_needed echo "$ELASTIC_PASSWORD" | elasticsearch-keystore add -x 'bootstrap.password')
    fi
  fi
fi

if [[ "$(id -u)" == "0" ]]; then
  # If requested and running as root, mutate the ownership of bind-mounts
  if [[ -n "$TAKE_FILE_OWNERSHIP" ]]; then
    chown -R 1000:0 /usr/share/elasticsearch/{data,logs}
  fi
fi

run_as_other_user_if_needed \
  /usr/share/elasticsearch/bin/elasticsearch \
  -p /tmp/gdb_elastic.pid >&2 &

online() {
  # It's OK if it fails if web server is down.
  set +e
  local STARTTIME
  STARTTIME=$(date +%s)
  while true; do
    if curl --connect-timeout 10 --max-time 310 -X GET \
      "localhost:9200/_cluster/health?wait_for_status=yellow&timeout=300s&pretty"
    then
      # Cassandra is online
      break
    fi
    echo >&2 "Waiting for elastic"
    sleep 1
  done
  local ENDTIME
  ENDTIME=$(date +%s)
  echo >&2 "Cassandra up in $((ENDTIME - STARTTIME)) seconds"
  set -e
}

online
