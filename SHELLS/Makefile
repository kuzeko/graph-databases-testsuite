MODULES:=arangodb janusgraph neo4j orientdb sqlgpg

help: ## Print this help message
	@echo "\033[36mModules:\033[0m  ${MODULES}"
	@grep -E '^[a-zA-Z0-9$$()_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

#MODULES := $(shell set -x; mvn help:evaluate -Dexpression=project.modules | grep -e 'shell-' | grep -e 'SKIPPED' | awk -F' ' '{print $2}' | awk -F '-' '{print $2}')

all: clean dist	## Build all shells
	@time mvn package
	@cp -v shell/*/target/shell-*-with-dependencies.jar dist

dist:
	@mkdir -p ./dist

clean:
	@rm -rvf shell/*/target/shell-*-with-dependencies.jar ./dist

$(MODULES): dist ## Build a single shell
	@time mvn -pl shell/shell-$@ -am package
	@cp -v shell/shell-$@/target/shell-*-with-dependencies.jar ./dist

utils: ## Build benchmark utilities (e.g. dataset sanitizer)
	@time mvn -pl utils -am package
	@cp -v utils/target/utils-*-jar-with-dependencies.jar  ./dist

.PHONY: build clean utils $(MOUDLES) 
