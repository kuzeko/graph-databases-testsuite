FROM  openjdk:8
LABEL authors="Brugnara <martin.brugnara@gmail.com>, Matteo Lissandrini <ml@disi.unitn.eu>, Nolan Nichols <nolan.nichols@gmail.com>"

RUN gpg --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys CD8CB0F1E0AD5B52E93F41E7EA93F5E56E751E9B

ENV GREMLIN3_TAG 3.2.4

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
        unzip \
        ant \
        git-core

RUN curl -L -o /tmp/gremlin.zip \
    http://mirror.nohup.it/apache/tinkerpop/${GREMLIN3_TAG}/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG}-bin.zip && \
    unzip /tmp/gremlin.zip -d /opt/ && \
    rm /tmp/gremlin.zip && \
    ln -s /opt/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG} ${GREMLIN3_HOME}


WORKDIR /tmp

COPY extra/.groovy /root/.groovy
COPY extra/*-neo4j-tp3.groovy /tmp/

RUN  ${GREMLIN3_HOME}/bin/gremlin.sh -e /tmp/install-neo4j-tp3.groovy && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-swing-2.4.*.jar && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-jsr223-2.4.*-indy.jar && \
     rm -vf ${GREMLIN3_HOME}/lib/groovy-xml-2.4.*.jar && \
     ${GREMLIN3_HOME}/bin/gremlin.sh -e /tmp/activate-neo4j-tp3.groovy



ADD init/neo4j-tp3-init.sh /
RUN chmod 755 /neo4j-tp3-init.sh

# standard port
#EXPOSE 9999

WORKDIR /runtime

CMD ["/neo4j-tp3-init.sh"]
#CMD ["bash"]
