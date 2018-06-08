FROM dbtrento/gremlin-neo4j
MAINTAINER Brugnara <mb@disi.unitn.eu>

# Add personalized init script
COPY init/2to3.sh /
RUN chmod 755 /2to3.sh

WORKDIR /runtime
CMD ["/2to3.sh"]
