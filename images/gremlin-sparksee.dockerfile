FROM anapsix/alpine-java:7_server-jre
MAINTAINER Brugnara <mb@disi.unitn.eu>

# This image is not longer available.
# FROM airdock/oracle-jdk:jre-1.7

ENV GREMLIN_TAG=2.6.0

RUN apk update && \
    apk add libuuid && \
    rm -rf /var/cache/*

# NOTE: automagically estracts.
ADD /libs/gremlin-groovy-${GREMLIN_TAG}.tgz /opt
RUN ln -s /opt/gremlin-groovy-${GREMLIN_TAG} /opt/gremlin


ENV GREMLIN_HOME=/opt/gremlin
ENV PATH=${GREMLIN_HOME}/bin:$PATH 

WORKDIR /runtime
CMD ["/runtime/tp2/execute.sh"]
