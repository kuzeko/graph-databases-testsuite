FROM airdock/oracle-jdk:jre-1.7
MAINTAINER Brugnara <mb@disi.unitn.eu>

ENV GREMLIN_TAG=2.6.0

# NOTE: automagically estracts.
ADD /libs/gremlin-groovy-${GREMLIN_TAG}.tgz /opt
RUN ln -s /opt/gremlin-groovy-${GREMLIN_TAG} /opt/gremlin

ENV GREMLIN_HOME=/opt/gremlin
ENV PATH=${GREMLIN_HOME}/bin:$PATH 

WORKDIR /runtime
CMD ["/runtime/tp2/execute.sh"]
