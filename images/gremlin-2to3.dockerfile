FROM anapsix/alpine-java:jdk7

MAINTAINER Brugnara <martin.brugnara@gmail.com>

WORKDIR /

ENV GREMLIN2_TAG 2.6.0

ENV GREMLIN2_HOME /opt/gremlin2

ENV PATH=${GREMLIN2_HOME}/bin:$PATH

RUN apk add --update curl ca-certificates

RUN curl -L -o /tmp/gremlin.zip \
        tinkerpop.com/downloads/gremlin/gremlin-groovy-${GREMLIN2_TAG}.zip && \
    unzip /tmp/gremlin.zip -d /opt/ && \
    rm /tmp/gremlin.zip && \
    mv /opt/gremlin-groovy-${GREMLIN2_TAG}/bin/gremlin.sh  /opt/gremlin-groovy-${GREMLIN2_TAG}/bin/gremlin2.sh && \
    ln -s /opt/gremlin-groovy-${GREMLIN2_TAG} /opt/gremlin2

ADD init/2to3.sh /
RUN chmod 755 /2to3.sh


WORKDIR /runtime


CMD ["/2to3.sh"]
