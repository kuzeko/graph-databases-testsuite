help:
	@echo "Docker images builder"
	@tail -n+4 Makefile

DFILES=$(wildcard *.dockerfile)

# Builder for '.dockerfile'
$(DFILES): 
	docker build -t dbtrento/$(basename $@) -f $@ .


# Images with deps
gremlin-2to3.dockerfile: gremlin-neo4j.dockerfile
	docker build -t dbtrento/$(basename $@) -f $@ .


common: gremlin-arangodb.dockerfile gremlin-blazegraph.dockerfile gremlin-janus-tp3.dockerfile gremlin-neo4j-tp3.dockerfile gremlin-neo4j.dockerfile gremlin-orientdb.dockerfile gremlin-pg.dockerfile gremlin-sparksee.dockerfile gremlin-titan-tp3.dockerfile gremlin-titan.dockerfile
	@echo "Building everithing"

.PHONY: $(DFILES) common buildit
