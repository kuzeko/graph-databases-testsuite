# Based on Debian 9
FROM openjdk:8
MAINTAINER Lissandrini <ml@disi.unitn.eu>

ENV JANUS_VERSION 0.2.0

RUN apt-get -qq update && \
    apt-get -qq upgrade  -y && \
    apt-get -qq install -y --no-install-recommends \
        openssl \
        curl \
        bash \
        gcc \
        zip \
        unzip

WORKDIR /tmp
RUN curl -L -o /tmp/janusgraph.zip \
        https://github.com/JanusGraph/janusgraph/releases/download/v${JANUS_VERSION}/janusgraph-${JANUS_VERSION}-hadoop2.zip && \
        unzip -q /tmp/janusgraph.zip -d /opt && \
        rm /tmp/janusgraph.zip && \
        ln -s /opt/janusgraph-${JANUS_VERSION}-hadoop2 /opt/janusgraph

# Setup Janus
ENV JANUS_HOME /opt/janusgraph
ENV PATH $JANUS_HOME/bin:$PATH
WORKDIR $JANUS_HOME
ENV JANUS_PROPERTIES=$JANUS_HOME/conf/janusgraph-cassandra-embedded.properties
ENV CASSANDRA_PROPERTIES=$JANUS_HOME/conf/cassandra/cassandra.yaml

# Gremling & java
ENV GREMLIN3_HOME $JANUS_HOME
RUN export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")


# Clean up default config and import our own
RUN rm -v ${JANUS_PROPERTIES} &&\
    rm -v ${CASSANDRA_PROPERTIES}

COPY extra/janusgraph-${JANUS_VERSION}-cassandra-embedded.properties ${JANUS_PROPERTIES}
COPY extra/janusgraph-${JANUS_VERSION}-cassandra.yaml ${CASSANDRA_PROPERTIES}

RUN rm $JANUS_HOME/lib/logback-classic-*

# Fix paths
RUN sed -i'.bak' 's@db/cassandra@'${JANUS_HOME}'/db/cassandra@g' "$CASSANDRA_PROPERTIES"

# Fix logging
RUN sed -i'.bak' 's@conf/log4j-console.properties -Dgremlin.log4j.level=\$GREMLIN_LOG_LEVEL@file://'${JANUS_HOME}'/conf/log4j-console.properties \${JANUS_JAVA_OPTS}@' "$JANUS_HOME/bin/gremlin.sh"

# Let it scale
# SPEED UP LOADING : DISABLE DEFAULT-SCHEMA
# To enable this, schema should be declared a-priori.
# @see /extract_schema.go
RUN printf "\nids.authority.wait-time=3000\n" >> "$JANUS_PROPERTIES" \
    && printf "\nstorage.batch-loading=true\n" >> "$JANUS_PROPERTIES" \
    && printf "\nschema.default=none\n" >> $JANUS_PROPERTIES

RUN  echo "memlock unlimited" >> /etc/security/limits.conf \
    && echo "nofile 100000"  >> /etc/security/limits.conf \
    && echo "nproc 32768"    >> /etc/security/limits.conf \
    && echo "as unlimited"   >> /etc/security/limits.conf \
    && tail -n 5 /etc/security/limits.conf  \
    && sysctl -p

# Prepare data directories
RUN mkdir -p $JANUS_HOME/db/cassandra/data \
    mkdir -p $JANUS_HOME/db/cassandra/commitlog \
    mkdir -p $JANUS_HOME/db/cassandra/saved_caches

# Add schema extractor (it dies bulding schema on the fly otherwise).
RUN apt-get -qq install -y golang
COPY extra/extract_schema.go /

COPY extra/dot_groovy /root/.groovy

# Include custom init srcipt
COPY init/janusgraph-${JANUS_VERSION}-init.sh /janusgraph-init.sh
RUN chmod 755 /janusgraph-init.sh

# Include custom queries
COPY extra/janusgraph-${JANUS_VERSION}-create-schema.groovy /janusgraph-create-schema.groovy
COPY extra/janusgraph-${JANUS_VERSION}-create-index.groovy  /janusgraph-create-index.groovy
COPY extra/janusgraph-${JANUS_VERSION}-drop-index.groovy    /janusgraph-drop-index.groovy

WORKDIR /runtime
CMD ["/janusgraph-init.sh"]
