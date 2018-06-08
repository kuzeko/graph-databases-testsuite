FROM  openjdk:8
LABEL authors="Brugnara <mb@disi.unitn.eu>, Matteo Lissandrini <ml@disi.unitn.eu>, Nolan Nichols <nolan.nichols@gmail.com>"

RUN gpg --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys CD8CB0F1E0AD5B52E93F41E7EA93F5E56E751E9B

ENV BLAZE_VERSION 2_1_4
ENV BLAZE_JAR blazegraph-jar-2.1.4.jar

ENV GREMLIN2_TAG 2.6.0
ENV GREMLIN3_TAG 3.2.9

ENV GREMLIN2_HOME /opt/gremlin2
ENV GREMLIN3_HOME /opt/gremlin

ENV PATH /opt/gremlin/bin:$PATH


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
        ant \
        git-core

#RUN curl -L -o /tmp/gremlin.zip \
#    http://mirror.nohup.it/apache/tinkerpop/${GREMLIN3_TAG}/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG}-bin.zip && \
ADD libs/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG}-bin.tgz /opt
RUN ln -s /opt/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG} ${GREMLIN3_HOME}

#RUN curl -L -o /tmp/gremlin.zip \
#    tinkerpop.com/downloads/gremlin/gremlin-groovy-${GREMLIN2_TAG}.zip && \
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

# standard port
# EXPOSE 9999

WORKDIR /runtime
CMD ["/blazegraph-init.sh"]
