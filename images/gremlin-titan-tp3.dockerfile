# Based on Debian 9
FROM  openjdk:8

MAINTAINER Lissandrini <ml@disi.unitn.eu>

ENV TITAN_VERSION 1.0.0
RUN apt-get -qq update && \
    apt-get -qq upgrade  -y && \
    apt-get -qq install  -y --no-install-recommends \
        openssl \
        curl \
        bash \
        zip \
        unzip \
        gcc

WORKDIR /tmp

RUN wget -q -O /tmp/titan.zip http://s3.thinkaurelius.com/downloads/titan/titan-${TITAN_VERSION}-hadoop1.zip
RUN mkdir -p /opt
RUN unzip -q /tmp/titan.zip -d /opt && rm /tmp/titan.zip


ENV TITAN_HOME /opt/titan-${TITAN_VERSION}-hadoop1
ENV GREMLIN3_HOME $TITAN_HOME
ENV PATH $TITAN_HOME/bin:$PATH

WORKDIR $TITAN_HOME

RUN export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::") && \
    echo $JAVA_HOME

ENV TITAN_PROPERTIES=$TITAN_HOME/conf/titan-cassandra-embedded.properties \
	CASSANDRA_PROPERTIES=$TITAN_HOME/conf/cassandra/cassandra.yaml

RUN rm -v ${TITAN_PROPERTIES} &&\
    rm -v ${CASSANDRA_PROPERTIES}

COPY extra/titan-${TITAN_VERSION}-cassandra-embedded.properties ${TITAN_PROPERTIES}
COPY extra/titan-${TITAN_VERSION}-cassandra.yaml ${CASSANDRA_PROPERTIES}


RUN rm $TITAN_HOME/lib/logback-classic-*

RUN sed -i'.bak' 's@db/cassandra@'${TITAN_HOME}'/db/cassandra@g' "$CASSANDRA_PROPERTIES"
RUN sed -i'.bak' 's@conf/log4j-console.properties -Dgremlin.log4j.level=\$GREMLIN_LOG_LEVEL@file://'${TITAN_HOME}'/conf/log4j-console.properties \${TITAN_JAVA_OPTS}@' "$TITAN_HOME/bin/gremlin.sh"

# http://s3.thinkaurelius.com/docs/titan/1.0.0/bulk-loading.html
# SPEED UP LOADING : DISABLE DEFAULT-SCHEMA
# To enable this, schema should be declared a-priori.
# @see /extract_schema.go
RUN printf "\nids.authority.wait-time=3000\n" >> "$TITAN_PROPERTIES" \
    && printf "\nstorage.batch-loading=true\n" >> "$TITAN_PROPERTIES" \
    && printf "\nschema.default=none\n" >> $TITAN_PROPERTIES


RUN  echo "memlock unlimited" >> /etc/security/limits.conf \
	&& echo "nofile 100000"  >> /etc/security/limits.conf \
	&& echo "nproc 32768"    >> /etc/security/limits.conf \
	&& echo "as unlimited"   >> /etc/security/limits.conf \
    && tail -n 5 /etc/security/limits.conf  \
    && sysctl -p


RUN mkdir -p $TITAN_HOME/db/cassandra/data \
	mkdir -p $TITAN_HOME/db/cassandra/commitlog \
	mkdir -p $TITAN_HOME/db/cassandra/saved_caches

COPY extra/extract_schema.go /
RUN apt-get -qq install -y golang

COPY init/titan-${TITAN_VERSION}-init.sh /titan-init.sh
RUN chmod 755 /titan-init.sh

COPY extra/dot_groovy /root/.groovy

COPY extra/titan-${TITAN_VERSION}-create-schema.groovy /titan-create-schema.groovy
COPY extra/titan-${TITAN_VERSION}-create-index.groovy  /titan-create-index.groovy
COPY extra/titan-${TITAN_VERSION}-drop-index.groovy    /titan-drop-index.groovy

WORKDIR /runtime
CMD ["/titan-init.sh"]
