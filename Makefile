help: ## Print this help message
	@grep -E '^[a-zA-Z0-9$$()_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


PHONY: init 
init: ## boostrap experimental env
	@echo "Step 1 - Building docker images"
	cd IMAGES && $(MAKE) all
	@echo "Step 2 - Building test-suit for supported systems"
	cd SHELLS && $(MAKE) all
	@echo "Step 3 - Prepare python env"
	cd CONTROL && $(MAKE) .venv
	@echo "Step 4 - Generating configuration template"
	cd CONTROL && \
		mkdir -p runtime/{data,logs,samples,schemas} && \
		cp -v ../SHELLS/common/src/main/resources/tinkerpop-modern_mod.json runtime/data/ && \
		source .venv/bin/activate && \
		python control.py generate-config \
			--config conf.toml \
			--runtime_dir ./runtime \
			--dataset_dir ./runtime/data \
			--shell_dir ../SHELLS/dist
