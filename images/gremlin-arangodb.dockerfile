#FROM debian:jessie
#FROM anapsix/alpine-java:jdk7
FROM  openjdk:8
MAINTAINER Brugnara <martin.brugnara@gmail.com>

RUN gpg --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys CD8CB0F1E0AD5B52E93F41E7EA93F5E56E751E9B

ENV ARCHITECTURE amd64
#ENV ARANGO_VERSION 2.7.7
ENV ARANGO_VERSION 2.8.11
ENV ARANGO_URL https://www.arangodb.com/repositories/arangodb2/Debian_8.0
ENV ARANGO_PACKAGE arangodb_${ARANGO_VERSION}_${ARCHITECTURE}.deb
ENV ARANGO_PACKAGE_URL ${ARANGO_URL}/${ARCHITECTURE}/${ARANGO_PACKAGE}
ENV ARANGO_SIGNATURE_URL ${ARANGO_PACKAGE_URL}.asc


ENV GREMLIN_TAG 2.6.0
ENV PATH /opt/gremlin/bin:$PATH
ENV GREMLIN_HOME /opt/gremlin



RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        build-essential \
        libstdc++6 \
        libgoogle-perftools4 \
        ca-certificates \
        pwgen \
        wget \
        openssl \
        golang \
        curl \
        bash \
        maven \
        unzip \
    && \
    wget ${ARANGO_PACKAGE_URL} &&         \
    dpkg -i ${ARANGO_PACKAGE} && \
    sed -ri \
        -e 's!127\.0\.0\.1!0.0.0.0!g' \
        -e 's!^(file\s*=).*!\1 -!' \
        -e 's!^#\s*uid\s*=.*!uid = arangodb!' \
        -e 's!^#\s*gid\s*=.*!gid = arangodb!' \
        /etc/arangodb/arangod.conf \
    && \
    rm -f ${ARANGO_PACKAGE}*

RUN curl -L -o /tmp/gremlin.zip \
        tinkerpop.com/downloads/gremlin/gremlin-groovy-${GREMLIN_TAG}.zip && \
    unzip /tmp/gremlin.zip -d /opt/ && \
    rm /tmp/gremlin.zip && \
    ln -s /opt/gremlin-groovy-${GREMLIN_TAG} /opt/gremlin

RUN cd /opt && \
    git clone https://github.com/arangodb/blueprints-arangodb-graph.git  && \
    cd blueprints-arangodb-graph/ && \
    mvn clean install -Dmaven.test.skip=true  -Dgpg.skip && \
    cp target/blueprints-arangodb-graph-*-standalone.jar /opt/gremlin/lib

#RUN rm /opt/gremlin-groovy-2.6.0/lib/logback-classic-*
RUN rm /opt/gremlin-groovy-2.6.0/lib/slf4j-log4j12*
RUN chmod 777 /etc/arangodb

#RUN arangod --daemon --pid-file /etc/arangodb/arangodb.pid
#RUN arangod > /tmp/arangod-server.log 2>&1 &

ADD extra/arangodb_converter.go /main.go
RUN chmod 755 /main.go

ADD init/arangodb-init.sh /
RUN chmod 755 /arangodb-init.sh


# standard port
EXPOSE 8529

WORKDIR /runtime

CMD ["/arangodb-init.sh"]
