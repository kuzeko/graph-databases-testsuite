FROM anapsix/alpine-java:jdk7

MAINTAINER Brugnara <martin.brugnara@gmail.com>

WORKDIR /

ENV GREMLIN_TAG=2.6.0 \
    PATH=/opt/gremlin/bin:$PATH \
    GREMLIN_HOME=/opt/gremlin

RUN apk add --update curl ca-certificates && \
    curl -L -o /tmp/gremlin.zip \
        tinkerpop.com/downloads/gremlin/gremlin-groovy-${GREMLIN_TAG}.zip && \
    unzip /tmp/gremlin.zip -d /opt/ && \
    rm /tmp/gremlin.zip && \
    ln -s /opt/gremlin-groovy-${GREMLIN_TAG} /opt/gremlin

WORKDIR /runtime


CMD ["/runtime/tp2/execute.sh"]
