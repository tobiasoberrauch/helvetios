#!/usr/bin/env bash
# T229 — Render the Structurizr DSL workspace into Mermaid (GitHub-rendered) and PlantUML.
#
# Mermaid output goes into docs/architecture/diagrams/*.mmd (referenced from MkDocs and the
# walkthrough). PlantUML output goes into the same directory as *.puml so MkDocs can render it
# inline via the plantuml-markdown plugin.
#
# Usage:
#   ./tools/architecture/export.sh
#
# Requires: Docker (uses the official structurizr/cli image so we don't need a local Java
# install for this task).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKSPACE="${REPO_ROOT}/tools/architecture/workspace.dsl"
OUT_DIR="${REPO_ROOT}/docs/architecture/diagrams"
mkdir -p "${OUT_DIR}"

if ! [[ -f "${WORKSPACE}" ]]; then
  echo "ERROR: ${WORKSPACE} not found" >&2
  exit 1
fi

# Mermaid export (one .mmd per view).
docker run --rm -v "${REPO_ROOT}:/workspace" structurizr/cli \
  export -workspace /workspace/tools/architecture/workspace.dsl \
         -format mermaid \
         -output /workspace/docs/architecture/diagrams

# PlantUML export (one .puml per view).
docker run --rm -v "${REPO_ROOT}:/workspace" structurizr/cli \
  export -workspace /workspace/tools/architecture/workspace.dsl \
         -format plantuml \
         -output /workspace/docs/architecture/diagrams

echo "✓ wrote diagrams to ${OUT_DIR}"
ls -1 "${OUT_DIR}" | head -20
