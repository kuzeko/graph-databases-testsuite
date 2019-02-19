# Based on Debian 9
FROM openjdk:8
MAINTAINER Brugnara <mb@disi.unitn.eu>

#RUN gpg --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys CD8CB0F1E0AD5B52E93F41E7EA93F5E56E751E9B

ENV ARCHITECTURE amd64
ENV ARANGO_VERSION 2.8.11
# Build are available only for Debian 8
# --> see missing dep for libssl1.0.0 (Debian 9 provides libssl1.0.1)
ENV ARANGO_PACKAGE arangodb_${ARANGO_VERSION}_${ARCHITECTURE}.deb
ENV ARANGO_SIGNATURE ${ARANGO_PACKAGE}.asc

# --> fixing missing libssl1.0.0
# Taken from http://security.debian.org/debian-security/pool/updates/main/o/openssl/${LIBSSL_DEB}
ENV LIBSSL_DEB libssl1.0.0_1.0.1t-1+deb7u4_amd64.deb

ENV GREMLIN_TAG 2.6.0
ENV PATH /opt/gremlin/bin:$PATH
ENV GREMLIN_HOME /opt/gremlin

# The package is not anymore available from the website ... Loading it from the our repo.
ADD extra/pkg/$ARANGO_PACKAGE $ARANGO_PACKAGE
ADD extra/pkg/$ARANGO_SIGNATURE $ARANGO_SIGNATURE
#RUN gpg --verify $ARANGO_SIGNATURE

# Vendoring also this lib
ADD extra/pkg/$LIBSSL_DEB $LIBSSL_DEB

RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
        libgoogle-perftools4 \
        golang \
        curl \
        bash \
    && \
    dpkg -i ${LIBSSL_DEB} && \
    rm -f ${LIBSSL_DEB} && \
    dpkg -i ${ARANGO_PACKAGE} && \
    sed -ri \
        -e 's!127\.0\.0\.1!0.0.0.0!g' \
        -e 's!^(file\s*=).*!\1 -!' \
        -e 's!^#\s*uid\s*=.*!uid = arangodb!' \
        -e 's!^#\s*gid\s*=.*!gid = arangodb!' \
        /etc/arangodb/arangod.conf \
    && \
    rm -f ${ARANGO_PACKAGE}*

ADD libs/gremlin-groovy-${GREMLIN_TAG}.tgz /opt
RUN ln -s /opt/gremlin-groovy-${GREMLIN_TAG} /opt/gremlin

# Add blueprint connector blueprints-arangodb-graph-1.0.15-standalone.jar
# Originally built from https://github.com/arangodb/blueprints-arangodb-graph.git
ADD extra/pkg/blueprints-arangodb-graph-1.0.15-standalone.jar /opt/gremlin/lib/

RUN rm -f /opt/gremlin/lib/slf4j-log4j12*
RUN chmod 755 /etc/arangodb

COPY extra/arangodb-collections-extractor.go /main.go
RUN chmod 755 /main.go

COPY init/arangodb-init.sh /
RUN chmod 755 /arangodb-init.sh

WORKDIR /runtime

CMD ["/arangodb-init.sh"]
