APP_NAME        := Kakkonen
VERSION         := $(shell git rev-parse --short HEAD)

.PHONY: help
help: ## Show this help
	@echo "Makefile for '${APP_NAME}'\n"
	@fgrep -h "##" $(MAKEFILE_LIST) | \
	fgrep -v fgrep | sed -e 's/## */##/' | column -t -s##

.PHONY: format
format: ## Reformat clj(s)
	@clojure-lsp format
	@clojure-lsp clean-ns

.PHONY: outdated
outdated: ## Update clj(s) dependencies
	@clj -Moutdated

.PHONY: install
install: ## Build and install jar locally
	@clj -Mjar
	@clj -Spom
	@clj -Minstall

.PHONY: test-clj
test-clj: ## Run clj tests
	@bin/kaocha

.PHONY: test-cljs
test-cljs: ## Run cljs tests
	@bin/node

.PHONY: test
test: test-clj test-cljs ## Run clj(s) tests
