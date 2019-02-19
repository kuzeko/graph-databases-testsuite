############################################################
# Dockerfile to run an OrientDB (Graph) Container
############################################################

FROM java:openjdk-8-jdk-alpine

MAINTAINER Brugnara (mb@disi.unitn.eu) OrientDB LTD (info@orientdb.com)

# Override the orientdb download location with e.g.:
#   docker build -t mine --build-arg ORIENTDB_DOWNLOAD_SERVER=http://repo1.maven.org/maven2/com/orientechnologies/ .
ARG ORIENTDB_DOWNLOAD_SERVER

ENV ORIENTDB_VERSION 2.2.13
ENV ORIENTDB_DOWNLOAD_MD5 030bbf826830d3e8533ff7db332f2ad7
ENV ORIENTDB_DOWNLOAD_SHA1 ab8f759067a8787c6013d3f15b37f82c3d96784e

# Adding from local repository 
#ENV ORIENTDB_DOWNLOAD_URL ${ORIENTDB_DOWNLOAD_SERVER:-http://central.maven.org/maven2/com/orientechnologies}/orientdb-community/$ORIENTDB_VERSION/orientdb-community-$ORIENTDB_VERSION.tar.gz
COPY extra/pkg/orientdb-community-$ORIENTDB_VERSION.tar.gz /

RUN apk add --update tar bash perl \
    && rm -rf /var/cache/apk/*

#tar, untar and delete databases
RUN mkdir /orientdb \
  && echo "$ORIENTDB_DOWNLOAD_MD5 *orientdb-community-$ORIENTDB_VERSION.tar.gz" | md5sum -c - \
  && echo "$ORIENTDB_DOWNLOAD_SHA1 *orientdb-community-$ORIENTDB_VERSION.tar.gz" | sha1sum -c - \
  && ls \
  && tar -xvzf orientdb-community-$ORIENTDB_VERSION.tar.gz -C /orientdb --strip-components=1 \
  && rm orientdb-community-$ORIENTDB_VERSION.tar.gz \
  && rm -rf /orientdb/databases/* \
  && sed -i'.bak'  's/-Xmx512m -Dclient.ssl.enabled=false /-Dclient.ssl.enabled=false/' /orientdb/bin/console.sh \
  && sed -i 's/$SSL_OPTS \\/$SSL_OPTS $JAVA_OPTIONS \\/' /orientdb/bin/console.sh \
  && cat /orientdb/bin/console.sh

ENV ORIENTDB_HOME=/orientdb

ENV PATH /orientdb/bin:$PATH

# We want to commit also the data
#VOLUME ["/orientdb/backup", "/orientdb/databases", "/orientdb/config"]

WORKDIR /orientdb

#OrientDb binary
EXPOSE 2424

#OrientDb http
EXPOSE 2480

# Default command start the server
# CMD ["server.sh"]

COPY init/orientdb-init.sh /
RUN chmod 755 /orientdb-init.sh


WORKDIR /runtime
# Invoke testing env
CMD ["/orientdb-init.sh"]
