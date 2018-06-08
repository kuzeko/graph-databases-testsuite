# Based on Debian 9
FROM openjdk:8	
MAINTAINER Brugnara <mb@disi.unitn.eu>

RUN gpg --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys CD8CB0F1E0AD5B52E93F41E7EA93F5E56E751E9B

ENV ARCHITECTURE amd64
ENV ARANGO_VERSION 2.8.11
# Build are available only for Debian 8
# --> see missing dep for libssl1.0.0 (Debian 9 provides libssl1.0.1)
ENV ARANGO_URL https://www.arangodb.com/repositories/arangodb2/Debian_8.0
ENV ARANGO_PACKAGE arangodb_${ARANGO_VERSION}_${ARCHITECTURE}.deb
ENV ARANGO_PACKAGE_URL ${ARANGO_URL}/${ARCHITECTURE}/${ARANGO_PACKAGE}
ENV ARANGO_SIGNATURE_URL ${ARANGO_PACKAGE_URL}.asc
# --> fixing missing libssl1.0.0
ENV LIBSSL_DEB libssl1.0.0_1.0.1t-1+deb7u4_amd64.deb
ENV LIBSSL_URL http://security.debian.org/debian-security/pool/updates/main/o/openssl/${LIBSSL_DEB}

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
    && \
    wget ${LIBSSL_URL} && \
    dpkg -i ${LIBSSL_DEB} && \
    rm -f ${LIBSSL_DEB} && \
    wget ${ARANGO_PACKAGE_URL} && \
    dpkg -i ${ARANGO_PACKAGE} && \
    sed -ri \
        -e 's!127\.0\.0\.1!0.0.0.0!g' \
        -e 's!^(file\s*=).*!\1 -!' \
        -e 's!^#\s*uid\s*=.*!uid = arangodb!' \
        -e 's!^#\s*gid\s*=.*!gid = arangodb!' \
        /etc/arangodb/arangod.conf \
    && \
    rm -f ${ARANGO_PACKAGE}*

#RUN curl -L -o /tmp/gremlin.zip \
#        tinkerpop.com/downloads/gremlin/gremlin-groovy-${GREMLIN_TAG}.zip && \
ADD libs/gremlin-groovy-${GREMLIN_TAG}.tgz /opt
RUN ln -s /opt/gremlin-groovy-${GREMLIN_TAG} /opt/gremlin

RUN cd /opt && \
    git clone https://github.com/arangodb/blueprints-arangodb-graph.git  && \
    cd blueprints-arangodb-graph/ && \
    mvn clean install -Dmaven.test.skip=true  -Dgpg.skip && \
    cp target/blueprints-arangodb-graph-*-standalone.jar /opt/gremlin/lib

#RUN rm -f /opt/gremlin/lib/logback-classic-*
RUN rm -f /opt/gremlin/lib/slf4j-log4j12*
RUN chmod 755 /etc/arangodb

COPY extra/arangodb-collections-extractor.go /main.go
RUN chmod 755 /main.go

COPY init/arangodb-init.sh /
RUN chmod 755 /arangodb-init.sh

# standard port
# EXPOSE 8529

WORKDIR /runtime

CMD ["/arangodb-init.sh"]
