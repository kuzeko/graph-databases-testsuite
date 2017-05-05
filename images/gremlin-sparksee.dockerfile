FROM ubuntu:14.04

# Install Java.
RUN \
    echo oracle-java7-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
    apt-get update && \
    apt-get install -y software-properties-common && \
    add-apt-repository -y ppa:webupd8team/java && \
    apt-get update && \
    apt-get install -y oracle-java7-installer curl unzip && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/cache/oracle-jdk7-installer

# Define working directory.
WORKDIR /

# Define commonly used JAVA_HOME variable
ENV JAVA_HOME /usr/lib/jvm/java-7-oracle

# Install gremlin
ENV GREMLIN_TAG=2.6.0
RUN curl -L -o /tmp/gremlin.zip \
        tinkerpop.com/downloads/gremlin/gremlin-groovy-${GREMLIN_TAG}.zip && \
    unzip /tmp/gremlin.zip -d /opt/ && \
    rm /tmp/gremlin.zip && \
    ln -s /opt/gremlin-groovy-${GREMLIN_TAG} /opt/gremlin
ENV PATH=/opt/gremlin/bin:$PATH \
    GREMLIN_HOME=/opt/gremlin

WORKDIR /runtime


# Define default command.
CMD ["/runtime/tp2/execute.sh"]
