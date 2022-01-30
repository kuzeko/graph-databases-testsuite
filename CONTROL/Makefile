help: ## Print this help message
	@echo "> Container maneger"
	@grep -E '^[a-zA-Z0-9$$()_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.venv: ## Prepare python virtual env
	# OLD syntax virtualenv -p python3 .venv
	python3 -m venv .venv
	source .venv/bin/activate && pip install -r requirements.txt

# ------------------------------------------------------------------------------
# Images
rm_noname: ## Remove images without a name
	docker images | grep -e "^<none" | awk -F' ' '{print $$3}' | xargs docker rmi || echo "no image to remove"

rm_notag: ## Remove images without a tag
	docker images | grep -e "^<none>\s\+<none>" | awk -F' ' '{print $$3}' | xargs docker rmi || echo "no image to remove"

rm_data: ## Remove all data images
	@echo -n "Are you sure? [y/N] " && read ans && [ $${ans:-N} = y ]
	@echo -n "Really really sure? [y/N] " && read ans && [ $${ans:-N} = y ]
	docker images | grep -e "data.graphbenchmark.com" | awk -F' ' '{print $$3}' | xargs docker rmi || echo "no image to remove"


# ------------------------------------------------------------------------------
# Container
rm_dead: ## Remove all dead containers
	docker ps -a | grep -v ' Up ' | tail -n+2 | awk -F' ' '{print $$1}' | xargs docker rm || echo "no container to remove"

stop: ## Stop all running containers
	docker ps -a | grep ' Up ' | tail -n+1 | awk -F' ' '{print $$1}' | xargs docker stop || echo "no container to stop"

kill: ## Kill all running containers
	docker ps -a | grep ' Up ' | tail -n+1 | awk -F' ' '{print $$1}' | xargs docker kill || echo "no container to kill"


destroy: stop rm_dead rm_noname rm_data ## Destroy the world
	@echo "Nothing is anymore"

.PHONY: help rm_noname rm_notag rm_data rm_dead stop kill destroy
