# vim:set ft=dockerfile:
FROM openjdk:11-slim-buster

# Must use jdk:11
# Otherwise elasticsearch crashed with
# Caused by: java.security.AccessControlException: access denied ("java.lang.RuntimePermission" "accessClassInPackage.jdk.internal.vm.annotation")

LABEL authors="Brugnara <mb@disi.unitn.eu>, Lissandrini <ml@disi.unitn.eu>" \
      project="graphbenchmark.com" \
      com.graphbenchmark.version="2.0.0" \
      com.graphbenchmark.dbname="janusgraph"

# This image is a composition of
# https://github.com/docker-library/cassandra/blob/master/3.11/Dockerfile
# https://raw.githubusercontent.com/elastic/dockerfiles/v7.6.0/elasticsearch/Dockerfile

# explicitly set user/group IDs
RUN groupadd -r cassandra --gid=999 && \
    useradd -r -g cassandra --uid=999 cassandra
RUN mkdir -p /usr/share/elasticsearch && \
    groupadd -g 1000 elasticsearch  && \
    useradd -u 1000 -g 1000 -G 0 -d /usr/share/elasticsearch elasticsearch && \
    chmod 0775 /usr/share/elasticsearch && \
    chgrp 0 /usr/share/elasticsearch

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        gnupg dirmngr \
# solves warning: "jemalloc shared library could not be preloaded to speed up memory allocations"
        libjemalloc2 \
# free is used by cassandra-env.sh
        procps \
# "ip" is not required by Cassandra itself, but is commonly used in scripting Cassandra's configuration (since it is so fixated on explicit IP addresses)
        iproute2 \
# Cassandra will automatically use numactl if available
#   https://github.com/apache/cassandra/blob/18bcda2d4c2eba7370a0b21f33eed37cb730bbb3/bin/cassandra#L90-L100
#   https://github.com/apache/cassandra/commit/604c0e87dc67fa65f6904ef9a98a029c9f2f865a
        numactl \
# Elasticsearch
        gzip libvshadow-utils tar \
    ; \
    rm -rf /var/lib/apt/lists/*


# =============================================================================
# Cassandra main part

# grab gosu for easy step-down from root
ENV GOSU_VERSION 1.10
RUN set -eux; \
    savedAptMark="$(apt-mark showmanual)"; \
    apt-get update; \
    apt-get install -y --no-install-recommends ca-certificates wget; \
    rm -rf /var/lib/apt/lists/*; \
    wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture)"; \
    wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$(dpkg --print-architecture).asc"; \
    export GNUPGHOME="$(mktemp -d)"; \
#    gpg --batch --keyserver hkps://keys.openpgp.org --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4; \
#    gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu; \
    gpgconf --kill all; \
    rm -rf "$GNUPGHOME" /usr/local/bin/gosu.asc; \
    chmod +x /usr/local/bin/gosu; \
    apt-mark auto '.*' > /dev/null; \
    apt-mark manual $savedAptMark > /dev/null; \
    apt-get purge -y --auto-remove -o APT::AutoRemove::RecommendsImportant=false; \
    gosu nobody true

# https://wiki.apache.org/cassandra/DebianPackaging#Adding_Repository_Keys
ENV GPG_KEYS \
# gpg: key 0353B12C: public key "T Jake Luciani <jake@apache.org>" imported
    514A2AD631A57A16DD0047EC749D6EEC0353B12C \
# gpg: key FE4B2BDA: public key "Michael Shuler <michael@pbandjelly.org>" imported
    A26E528B271F19B9E5D8E19EA278B781FE4B2BDA \
    E91335D77E3E87CB
RUN set -eux; \
    export GNUPGHOME="$(mktemp -d)"; \
    for key in $GPG_KEYS; do \
        gpg --batch --keyserver ha.pool.sks-keyservers.net --recv-keys "$key"; \
    done; \
    gpg --batch --export $GPG_KEYS > /etc/apt/trusted.gpg.d/cassandra.gpg; \
    command -v gpgconf && gpgconf --kill all || :; \
    rm -rf "$GNUPGHOME"; \
    apt-key list

ENV CASSANDRA_VERSION 3.11.7

RUN set -eux; \
    \
# https://bugs.debian.org/877677
# update-alternatives: error: error creating symbolic link '/usr/share/man/man1/rmid.1.gz.dpkg-tmp': No such file or directory
    mkdir -p /usr/share/man/man1/; \
    echo 'deb http://www.apache.org/dist/cassandra/debian 311x main' > /etc/apt/sources.list.d/cassandra.list; \
    apt-get update; \
    apt-get install -y \
        cassandra="$CASSANDRA_VERSION" \
        cassandra-tools="$CASSANDRA_VERSION" \
    ; \
    \
    rm -rf /var/lib/apt/lists/*;

ENV CASSANDRA_CONFIG /etc/cassandra

# https://issues.apache.org/jira/browse/CASSANDRA-11661
RUN sed -ri 's/^(JVM_PATCH_VERSION)=.*/\1=25/' "$CASSANDRA_CONFIG/cassandra-env.sh"

RUN mkdir -p /var/lib/cassandra "$CASSANDRA_CONFIG" \
    && chown -R cassandra:cassandra /var/lib/cassandra "$CASSANDRA_CONFIG" \
    && chmod 777 /var/lib/cassandra "$CASSANDRA_CONFIG"

COPY extra/janus/cassandra_config/jvm.options \
     extra/janus/cassandra_config/cassandra.yaml \
     extra/janus/cassandra_config/logback.xml \
     "$CASSANDRA_CONFIG"/

COPY extra/janus/cassandra.sh /
RUN chown 999:0 /cassandra.sh
RUN chmod 755 /cassandra.sh


# ==============================================================================
# Elasticsearch main part
ENV PATH /usr/share/elasticsearch/bin:$PATH

# Untill janus pubs the new version we are stuck to 6.6.0
# https://docs.janusgraph.org/changelog/#version-compatibility
# https://github.com/JanusGraph/janusgraph/commit/bc5e771895c328ff28aa0a84d6a543f5eb7153a5
WORKDIR /usr/share/elasticsearch
RUN set -eux; \
    apt-get update; \
    apt-get install -y wget curl; \
    cd /opt; \
    wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-6.6.0.tar.gz; \
    cd -; \
    tar zxf /opt/elasticsearch-6.6.0.tar.gz --strip-components=1; \
    rm -rf /var/lib/apt/lists/*;


# >=6.8
#RUN grep ES_DISTRIBUTION_TYPE=tar /usr/share/elasticsearch/bin/elasticsearch-env && \
#    sed -i -e 's/ES_DISTRIBUTION_TYPE=tar/ES_DISTRIBUTION_TYPE=docker/' /usr/share/elasticsearch/bin/elasticsearch-env

RUN mkdir -p config data logs && \
    chmod 0775 config data logs
# TODO: double check id does not copy dir in dir
COPY extra/janus/elastic_config/   /usr/share/elasticsearch/config/
RUN chmod 0660 config/elasticsearch.yml config/log4j2.properties

ENV ELASTIC_CONTAINER true

COPY extra/janus/elastic.sh /
RUN chown 1000:0 /elastic.sh
RUN chmod 775 /elastic.sh

# Logging
COPY extra/janus/logback.xml /

# ==============================================================================
# Common
COPY extra/janus/janus-init.sh   /entrypoint.sh
RUN chown 0:0 /entrypoint.sh
RUN chmod 0775 /entrypoint.sh

WORKDIR /
ENTRYPOINT ["/entrypoint.sh"]
CMD []
