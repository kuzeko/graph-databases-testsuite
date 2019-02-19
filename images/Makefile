help: ## Print this help message
	# @echo "> Graph Databases images builder"
	@grep -E '^[a-zA-Z0-9$$()_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

DFILES=$(wildcard *.dockerfile)

$(DFILES): ## Build any dbimage by its `.dockerfile` name
	docker build -t dbtrento/$(basename $@) -f $@ .

# Images with deps
gremlin-2to3.dockerfile: gremlin-neo4j.dockerfile
	docker build -t dbtrento/$(basename $@) -f $@ .

vldb19: gremlin-arangodb.dockerfile gremlin-blazegraph.dockerfile gremlin-neo4j.dockerfile gremlin-neo4j-tp3.dockerfile gremlin-orientdb.dockerfile gremlin-sparksee.dockerfile gremlin-titan.dockerfile gremlin-titan-tp3.dockerfile gremlin-pg.dockerfile ## Build all dbimages used for VLDB19 paper.

.PHONY: $(DFILES) common buildit
