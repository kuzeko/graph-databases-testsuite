help:
	@echo "Docker images builder"
	@tail -n+4 Makefile

DFILES=$(wildcard *.dockerfile)

# Builder for '.dockerfile'
$(DFILES):
	docker build -t dbtrento/$(basename $@) -f $@ .

.PHONY: $(DFILES)
