FROM openjdk:13-slim-buster
LABEL authors="Brugnara <mb@disi.unitn.eu>, Lissandrini <ml@disi.unitn.eu>" \
      project="graphbenchmark.com" \
      com.graphbenchmark.version="2.0.0" \
        com.graphbenchmark.dbname="arangodb"

# ArangoDB setup is based on official, but outdate, docker images.
# Debian stretch (9.x) and ArangoDB 3.3.23
# https://github.com/arangodb/arangodb-docker/blob/136819031744598b894ceb4e08bfb3db6cd7b129/stretch/3.3.23/Dockerfile
# Alpine ArangoDB 3.6
# https://github.com/arangodb/arangodb-docker/blob/136819031744598b894ceb4e08bfb3db6cd7b129/alpine/3.6.0/Dockerfile

# Note:
# For this project we do not need foxx, https://www.arangodb.com/docs/stable/foxx.html
# To configure logging see: https://www.arangodb.com/docs/3.6/programs-arangod-log.html

ENV ARCHITECTURE amd64
ENV DEB_PACKAGE_VERSION 1
ENV ARANGO_VERSION 3.6.0
ENV ARANGO_URL https://download.arangodb.com/arangodb36/DEBIAN/amd64
ENV ARANGO_PACKAGE arangodb3_${ARANGO_VERSION}-1_amd64.deb
ENV ARANGO_PACKAGE_URL ${ARANGO_URL}/${ARANGO_PACKAGE}
ENV ARANGO_SIGNATURE_URL ${ARANGO_PACKAGE_URL}.asc

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        dirmngr \
        gpg \
        gpg-agent \
    && rm -rf /var/lib/apt/lists/*

#RUN gpg --batch --keyserver hkps://hkps.pool.sks-keyservers.net --recv-keys CD8CB0F1E0AD5B52E93F41E7EA93F5E56E751E9B

# need at least
#   openssl to >= 1.1.0j-1~deb9u1
#   sensible-utils to >= 0.0.9+deb9u1
#   curl to >= 7.52.1-5+deb9u8
#   libgcrypt20 to >= 1.7.6-2+deb9u3
#   libtasn1-6 to >= 4.10-1.1+deb9u1

# NOTE: we libjemalloc1 has been replaced by libjemalloc2

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        libjemalloc2 \
        libtasn1-6 \
        numactl \
        openssl \
        sensible-utils \
    && rm -rf /var/lib/apt/lists/*


# see
#   https://docs.arangodb.com/latest/Manual/Administration/Configuration/Endpoint.html
#   https://docs.arangodb.com/latest/Manual/Administration/Configuration/Logging.html
#    gpg --verify ${ARANGO_PACKAGE}.asc && \

RUN curl --fail -O ${ARANGO_SIGNATURE_URL} &&       \
    curl --fail -O ${ARANGO_PACKAGE_URL} &&         \
    (echo arangodb3 arangodb3/password password test | debconf-set-selections) && \
    (echo arangodb3 arangodb3/password_again password test | debconf-set-selections) && \
    DEBIAN_FRONTEND="noninteractive" dpkg -i ${ARANGO_PACKAGE} && \
    rm -rf /var/lib/arangodb3/* && \
    sed -ri \
        -e 's!127\.0\.0\.1!0.0.0.0!g' \
        -e 's!^(file\s*=).*!\1 -!' \
        -e 's!^\s*uid\s*=.*!!' \
        /etc/arangodb3/arangod.conf \
    && chgrp 0 /var/lib/arangodb3 /var/lib/arangodb3-apps \
    && chmod 775 /var/lib/arangodb3 /var/lib/arangodb3-apps \
    && \
    rm -f ${ARANGO_PACKAGE}*
# Note that Openshift runs containers by default with a random UID and GID 0.
# We need that the database and apps directory are writable for this config.

# For this project we will "commit" the loaded dataset into an image.
# The database directory shall NOT be in volumes.
# VOLUME ["/var/lib/arangodb3", "/var/lib/arangodb3-apps"]

COPY extra/arangodb-init.sh /entrypoint.sh
RUN chgrp 0 /entrypoint.sh && \
    chmod 755 /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]   # Will invoke the shell
CMD []                          # Reset
