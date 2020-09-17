help: ## Print this help message
	@echo "> Graph Databases images builder"
	@grep -E '^[a-zA-Z0-9$$()_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

DFILES=$(wildcard *.dockerfile)

$(DFILES): base.dockerfile 	## Build any dbimage by its `.dockerfile` name
	docker build -t graphbenchmark.com/$(basename $@) -f $@ .

base.dockerfile: 	## Build the base docker image
	docker build -t graphbenchmark.com/$(basename $@) -f $@ .

all: $(DFILES) ## Build all images

.PHONY: $(DFILES)
