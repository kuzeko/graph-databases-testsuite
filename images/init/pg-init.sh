#!/bin/bash
# Specific Sqlg bootstrap
# maintainer: Brugnara, Lissandrini
#set -euo pipefail
IFS=$'\n\t'

touch ${RUNTIME_DIR}/errors.log
touch ${RUNTIME_DIR}/results.csv


if [[ -z ${JAVA_OPTS+x} ]]; then
   echo "NO JAVA_OPTS SET TO Postgresql TP3 - SETTING DEFAULT"
   export JAVA_OPTS='-Xms4g -Xmn128M -Xmx120g'

fi

echo "JAVA_OPTS=$JAVA_OPTS"
# export NATIVE_LOADING=True

export PGUSER="${PGUSER:-postgres}"
export POSTGRES_USER=${POSTGRES_USER:-postgres}
export POSTGRES_DB=${POSTGRES_DB:-$POSTGRES_USER}

echo "BEFORE INIT"

sudo -u postgres /bin/bash - <<PGEOF || echo "END SUBSHELL"
export PATH=/usr/lib/postgresql/9.6/bin:$PATH
if [[ "${QUERY}" == *loader.groovy ]]; then
  if [[ ! -f $DATASET ]]; then
     (>&2 echo "DATASET: '$DATASET' file does not exists.")
     exit 1
  fi

  mkdir -p "$PGDATA"
  chown -R "$(id -u)" "$PGDATA" 2>/dev/null || :
  chmod 700 "$PGDATA" 2>/dev/null || :

  # look specifically for PG_VERSION, as it is expected in the DB dir
  if [ ! -s "$PGDATA/PG_VERSION" ]; then
    #file_env 'POSTGRES_INITDB_ARGS'
    if [ "$POSTGRES_INITDB_XLOGDIR" ]; then
      export POSTGRES_INITDB_ARGS="$POSTGRES_INITDB_ARGS --xlogdir $POSTGRES_INITDB_XLOGDIR"
    fi
    eval "initdb --username=postgres $POSTGRES_INITDB_ARGS -D $PGDATA"

    # internal start of server in order to allow set-up using psql-client
    # does not listen on external TCP/IP and waits until start finishes
    pg_ctl -D "$PGDATA" \
      -o "-c listen_addresses='localhost'" \
      -w start

    psql=( psql -v ON_ERROR_STOP=1 )

    if [ "$POSTGRES_DB" != 'postgres' ]; then
      echo "CREATE DATABASE \"$POSTGRES_DB\" ;" | "${psql[@]}" --username postgres
    fi

    echo
    echo 'PostgreSQL init process complete; ready.'
    echo
  fi
else
  # Start db
  pg_ctl -D "$PGDATA" \
    -o "-c listen_addresses='localhost'" \
    -w start
fi

echo "PRE SUBSHELL RETURN"
PGEOF


if [[ "${QUERY}" == *loader.groovy ]] && [[ -z ${NOHASH+x} ]]; then
   echo "PG (hashing labels for) loading $DATASET" | tee -a ${RUNTIME_DIR}/errors.log

   #SOMETIMES We need to convert dataset labels
   while read line ; do
       md=`echo -n $line | md5sum | cut -f1 -d" "`
       echo 's@"_label":"'${line}'"@"_label":"'${md}'"''@g;' >> /tmp/replacements.txt
   done < <( cat $DATASET | tr ',' '\n' | grep -F '_label' | grep -o  '"_label":"[^"]*"'   | cut -f 2 -d':' | sed 's/"//g' | sort | uniq)

   wc -l /tmp/replacements.txt  | tee -a ${RUNTIME_DIR}/errors.log
   echo "PG (hashing labels for) loading $DATASET" | tee -a ${RUNTIME_DIR}/errors.log

   DATASET_NAME=$(basename "${DATASET}")
   SAFE_DATASET="/tmp/${DATASET_NAME}_hashed"

   # Checks
   # ls -lh $DATASET | tee -a ${RUNTIME_DIR}/errors
   # md5sum $DATASET | tee -a ${RUNTIME_DIR}/errors

   # GO MOD
   go run /pghash.go -i "${DATASET}" -o "${SAFE_DATASET}" -r "/tmp/replacements.txt"

   # Checks
   # ls -lh $SAFE_DATASET | tee -a ${RUNTIME_DIR}/errors
   # md5sum $SAFE_DATASET | tee -a ${RUNTIME_DIR}/errors
   # grep -Fc 'football_coach_position' $SAFE_DATASET | tee -a ${RUNTIME_DIR}/errors
   #ls -lah $DATASET | tee -a ${RUNTIME_DIR}/errors
   #ls -lah $SAFE_DATASET | tee -a ${RUNTIME_DIR}/errors

   DATASET=$SAFE_DATASET
   sed 's/#SQLG//g'  ${RUNTIME_DIR}/tp3/loader.groovy  >>  /tmp/bulkloader.groovy
   QUERY=/tmp/bulkloader.groovy
fi

echo "BEFORE QUERY"
export INDEX_QUERY="/pg-create-index.groovy"


# Run gremlin
. ${RUNTIME_DIR}/tp3/execute.sh

echo "AFTER QUERY"

if [[ "${QUERY}" == *loader.groovy ]] && [[ -z ${NOHASH+x} ]]; then
   rm -v "$SAFE_DATASET" | tee -a ${RUNTIME_DIR}/errors.log
fi

exec sudo -u postgres /bin/bash - <<PGEOF
  export PATH=/usr/lib/postgresql/9.6/bin:$PATH
  pg_ctl -D "$PGDATA" \
    -o "-c listen_addresses='localhost'" \
    -w stop
PGEOF
