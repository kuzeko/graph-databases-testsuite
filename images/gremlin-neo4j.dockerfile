FROM anapsix/alpine-java:jdk7
MAINTAINER Brugnara <mb@disi.unitn.eu>

WORKDIR /

ENV GREMLIN_TAG=2.6.0 \
    PATH=/opt/gremlin/bin:$PATH \
    GREMLIN_HOME=/opt/gremlin

ADD libs/gremlin-groovy-${GREMLIN_TAG}.tgz /opt
RUN ln -s /opt/gremlin-groovy-${GREMLIN_TAG} /opt/gremlin

WORKDIR /runtime
CMD ["/runtime/tp2/execute.sh"]
