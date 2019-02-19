FROM java:8-jre-alpine

MAINTAINER Brugnara <mb@disi.unitn.eu>

ENV TITAN_VERSION 0.5.4

RUN apk add --update linux-pam bash gcc ca-certificates openssl

RUN mkdir -p /opt
RUN cd /tmp \
    && wget https://disi.unitn.it/~brugnara/data/pkg/titan-${TITAN_VERSION}-hadoop2.zip \
    && wget https://disi.unitn.it/~brugnara/data/pkg/titan-${TITAN_VERSION}-hadoop2.zip.md5 \
    && md5sum -c titan-${TITAN_VERSION}-hadoop2.zip.md5 \
    && unzip -q titan-${TITAN_VERSION}-hadoop2.zip -d /opt \
    && rm /tmp/*hadoop*

ENV TITAN_HOME /opt/titan-$TITAN_VERSION-hadoop2
ENV PATH $TITAN_HOME/bin:$PATH
WORKDIR $TITAN_HOME

ENV TITAN_PROPERTIES=$TITAN_HOME/conf/titan-cassandra-embedded.properties \
	CASSANDRA_PROPERTIES=$TITAN_HOME/conf/cassandra.yaml


RUN sed -i'.bak' 's@num_tokens: 4@num_tokens: 512@;  s@db/cassandra@'${TITAN_HOME}'/db/cassandra@g' "$CASSANDRA_PROPERTIES"
RUN sed -i'.bak' 's@log4j-gremlin.properties@file:/runtime/confs/log4j-cassandra.properties -javaagent:'${TITAN_HOME}'/lib/jamm-0.2.5.jar @' "$TITAN_HOME/bin/gremlin.sh"

# http://s3.thinkaurelius.com/docs/titan/0.5.4/bulk-loading.html
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

RUN apk add --update go

COPY extra/extract_schema.go /

COPY init/titan-${TITAN_VERSION}-init.sh /titan-init.sh
RUN chmod 755 /titan-init.sh


COPY extra/titan-${TITAN_VERSION}-create-schema.groovy /titan-create-schema.groovy
COPY extra/titan-${TITAN_VERSION}-drop-index.groovy    /titan-drop-index.groovy
COPY extra/titan-${TITAN_VERSION}-re-index.groovy      /titan-re-index.groovy

ENV INDEX_QUERY_PREFIX="titan-0.5.4-"

WORKDIR /runtime
CMD ["/titan-init.sh"]
