help: ## Print this help message
	@grep -E '^[a-zA-Z0-9$$()_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


PHONY: all
all: images shells control runtime conf

images:
	@echo "Step 1 - Building docker images"
	cd IMAGES && $(MAKE) all

shells:
	@echo "Step 2 - Building test-suit for supported systems"
	cd SHELLS && $(MAKE) all
	
control:
	@echo "Step 3 - Prepare python env"
	cd CONTROL && $(MAKE) .venv


runtime: ## boostrap experimental env
	@echo "Step 4 - Preparing runtime directory"
	cd CONTROL && \
		mkdir -vp runtime/data && \
		mkdir -vp runtime/logs && \
		mkdir -vp runtime/samples && \
		mkdir -vp runtime/schemas && \
		cp -v ../SHELLS/common/src/main/resources/tinkerpop-modern_mod.json runtime/data/ &&
		cd ..


conf:
	@echo "Step 5 - Generating configuration template"
	cd CONTROL && \
	. .venv/bin/activate && \
		python control.py generate-config \
			--config conf.toml \
			--runtime_dir ./runtime \
			--dataset_dir ./runtime/data \
			--shell_dir ../SHELLS/dist

conf_neo4j:
	@echo "Step 5 - Generating configuration template"
	cd CONTROL && \
	. .venv/bin/activate && \
		python control.py generate-config \
			--config conf.toml \
			--runtime_dir ./runtime \
			--dataset_dir ./runtime/data \
			--shell_dir ../SHELLS/dist \
                        --database neo4j
