# Swiss Trading & Market Support Platform — Makefile shim.
#
# Der eigentliche Task-Runner ist Taskfile.yml (https://taskfile.dev).
# Dieser Makefile bleibt als Muscle-Memory-Convenience und delegiert an `task`.
#
# `task` installieren:
#   curl -sL https://taskfile.dev/install.sh | sh
#   (oder: mise install   — falls .mise.toml schon getrustet ist)

SHELL := /bin/bash
.DEFAULT_GOAL := help

.PHONY: help
help:  ## Liste alle Tasks (delegiert an `task -l`)
	@if command -v task >/dev/null 2>&1; then \
		task -l; \
	else \
		echo "✗ 'task' nicht installiert."; \
		echo "  Install:  curl -sL https://taskfile.dev/install.sh | sh"; \
		echo "  oder:     mise install"; \
	fi

# Stable Targets — delegieren an Task. `make smoke` etc. funktionieren weiter.

.PHONY: setup build test smoke lint format clean tilt-up tilt-down new-venue ptp-audit scaffold walkthrough ci constitution-check vendor-mirror docs-serve

setup:           ; task setup                              ## Einmaliges Onboarding
build:           ; task build                              ## Build everything
test:            ; task test                               ## Alle Tests
smoke:           ; task smoke                              ## End-to-End-Smoke
lint:            ; task lint                               ## Lint
format:          ; task format                             ## Auto-format
clean:           ; task clean                              ## Build-Artefakte aufräumen
tilt-up:         ; task tilt:up                            ## Tilt up
tilt-down:       ; task tilt:down                          ## Tilt down
ptp-audit:       ; task ptp-audit                          ## RTS-25 Audit-Pack (Demo)
scaffold:        ; task scaffold                           ## SBE/JAXB codegen
walkthrough:     ; task walkthrough                        ## 30-Min-Interview-Walkthrough
ci:              ; task ci                                 ## Lokale CI-Reproduktion
constitution-check: ; task constitution:check              ## Verfassungs-Gates lokal
vendor-mirror:   ; task vendor-mirror                      ## Vendor-JAR-Hinweise
docs-serve:      ; task docs:serve                         ## MkDocs lokal

new-venue:  ## Scaffold neuen Venue-Adapter. Usage: make new-venue NAME=cboe
	@if [ -z "$(NAME)" ]; then echo "Usage: make new-venue NAME=<lowercase>"; exit 1; fi
	task new-venue NAME=$(NAME)
