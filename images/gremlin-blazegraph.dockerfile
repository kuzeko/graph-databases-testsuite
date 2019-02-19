FROM  openjdk:8
LABEL authors="Brugnara <mb@disi.unitn.eu>, Matteo Lissandrini <ml@disi.unitn.eu>, Nolan Nichols <nolan.nichols@gmail.com>"

ENV BLAZE_VERSION 2_1_4
ENV BLAZE_JAR blazegraph-jar-2.1.4.jar

ENV GREMLIN2_TAG 2.6.0
ENV GREMLIN3_TAG 3.2.9

ENV GREMLIN2_HOME /opt/gremlin2
ENV GREMLIN3_HOME /opt/gremlin

ENV PATH /opt/gremlin/bin:$PATH

ADD libs/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG}-bin.tgz /opt
RUN ln -s /opt/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG} ${GREMLIN3_HOME}

ADD libs/gremlin-groovy-${GREMLIN2_TAG}.tgz /opt
RUN mv /opt/gremlin-groovy-${GREMLIN2_TAG}/bin/gremlin.sh  /opt/gremlin-groovy-${GREMLIN2_TAG}/bin/gremlin2.sh && \
    ln -s /opt/gremlin-groovy-${GREMLIN2_TAG} ${GREMLIN2_HOME}

WORKDIR /tmp

COPY extra/dot_groovy /root/.groovy
COPY extra/*-blazegraph.groovy /tmp/

RUN  ${GREMLIN3_HOME}/bin/gremlin.sh -e /tmp/install-blazegraph.groovy && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-swing-2.4.*.jar && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-jsr223-2.4.*-indy.jar && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-xml-2.4.*.jar && \
     ${GREMLIN3_HOME}/bin/gremlin.sh -e /tmp/activate-blazegraph.groovy

COPY extra/BlazegraphRepositoryProvider.groovy /tmp/
COPY extra/blazegraph-config.groovy /tmp/

COPY init/blazegraph-init.sh /
RUN chmod 755 /blazegraph-init.sh

WORKDIR /runtime
CMD ["/blazegraph-init.sh"]
