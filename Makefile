help:
	@echo "Results"
	@echo "\t follow: \t follows all logs stream"
	@echo "\t follow_short: \t follows result and error logs stream"
	@echo "\t collect: \t move results and log into COLLECTED folder"
	@echo "\t rm_log: \t remove logs, but do not touch containers"
	@echo "\t clean: \t clean up environement for next experiment"
	@echo "\t purge: \t like clean but remove (not collect) the results"
	@echo ""
	@echo "Containers management command"
	@echo "\t rm_dead: \t Remove dead containers"
	@echo "\t stop:    \t Stop all running containers"
	@echo "\t kill:    \t Kill all running containers"
	@echo ""
	@echo "Image management command"
	@echo "\t rm_notag:\t Remove images without a tag"
	@echo "\t rm_noname:\t Remove images without a name"
	@echo ""
	@echo "Dangerous management command"
	@echo "\t destroy:\t Reset the docker installation."
	@echo "\t         \t rm -rf images and containers"


# ------------------------------------------------------------------------------
# Images
rm_noname:
	docker images | grep -e "^<none" | awk -F' ' '{print $$3}' | xargs docker rmi || echo "no image to remove"

rm_notag:
	docker images | grep -e "^<none>\s\+<none>" | awk -F' ' '{print $$3}' | xargs docker rmi || echo "no image to remove"


# ------------------------------------------------------------------------------
# Container
rm_dead:
	docker ps -a | grep -v ' Up ' | tail -n+2 | awk -F' ' '{print $$1}' | xargs docker rm || echo "no container to remove"

stop:
	docker ps -a | grep ' Up ' | tail -n+1 | awk -F' ' '{print $$1}' | xargs docker stop || echo "no container to stop"

kill:
	docker ps -a | grep ' Up ' | tail -n+1 | awk -F' ' '{print $$1}' | xargs docker kill || echo "no container to kill"


# Destroy the world
destroy: stop rm_dead
	docker images | tail -n+2 | awk -F' ' '{print $$3}' | xargs docker rmi -f || echo "no image to remove"


# ------------------------------------------------------------------------------
# Results management

collect:
	@echo "Collecting results"
	./collect.sh

clean: stop kill rm_dead rm_notag collect
	#

rm_log:
	rm -fv timeout.log docker.log test.log runtime/results.csv runtime/errors.log runtime/logs/* runtime/debug.log

purge: stop kill rm_dead rm_notag rm_log
    #

# ------------------------------------------------------------------------------
# logs
follow:
	tail -f *.log runtime/errors.log runtime/results.csv runtime/*.log

follow_short:
	tail -f runtime/errors.log runtime/results.csv runtime/*.log

.PHONY: help rm_noname rm_notag rm_dead stop kill destroy collect clean
