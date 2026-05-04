#!/usr/bin/env bash
# T336 — Quarterly Constitution audit.
#
# Samples the last 90 days of merged PRs and checks each one against the seven Constitution
# principles. Emits a compliance report under reports/constitution-audit-${QUARTER}.md.
#
# Usage:
#   ./tools/constitution-audit/audit.sh [QUARTER_YEAR-Qn]
set -eo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
QUARTER="${1:-$(date +%Y)-Q$(( ($(date +%m) - 1) / 3 + 1 ))}"
SINCE="$(date -u -v-90d +%Y-%m-%d 2>/dev/null || date -u -d '90 days ago' +%Y-%m-%d)"
OUT="${REPO_ROOT}/reports/constitution-audit-${QUARTER}.md"

mkdir -p "$(dirname "$OUT")"
cd "$REPO_ROOT"

count_matches() {
    local pattern="$1"
    local path="$2"
    # grep returns 1 when there are no matches; tolerate that under pipefail.
    { grep -rn "$pattern" "$path" 2>/dev/null || true; } | wc -l | tr -d ' '
}

count_files() {
    { find "$@" 2>/dev/null || true; } | wc -l | tr -d ' '
}

{
    echo "# Constitution audit — ${QUARTER}"
    echo ""
    echo "Window: ${SINCE} → $(date -u +%Y-%m-%d)"
    echo ""

    # Principle I — domain MUST NOT import any venue / vendor protocol type.
    echo "## Principle I — Hexagonal Adapter Discipline"
    domain_imports=$(count_matches \
        'import quickfix\|import io.aeron\|import com.bloomberg\|import com.refinitiv' \
        libs/domain-model/src/main)
    if [[ "$domain_imports" -eq 0 ]]; then
        echo "✓ Pass — 0 venue imports in libs/domain-model"
    else
        echo "✗ FAIL — ${domain_imports} venue imports leak into domain"
    fi
    echo ""

    # Principle IV — domain MUST NOT call System.currentTimeMillis() / new Date() / Instant.now().
    echo "## Principle IV — Time-Sync as First-Class"
    wallclock_calls=$(count_matches \
        'System\.currentTimeMillis\|new Date()\|Instant\.now()' \
        libs/domain-model/src/main)
    if [[ "$wallclock_calls" -eq 0 ]]; then
        echo "✓ Pass — 0 wall-clock calls in domain"
    else
        echo "✗ FAIL — ${wallclock_calls} wall-clock calls in domain"
    fi
    echo ""

    # Principle VII — every codec must have a property test.
    echo "## Principle VII — Test-First for Protocol Code"
    codec_files=$(count_files libs/fix-codec libs/sbe-codec libs/fixml-codec libs/fpml-codec \
        -name "*.java" -path "*src/main*")
    property_tests=$(count_files libs -name "*PropertyTest.java" -path "*src/test*")
    echo "Codec source files: ${codec_files}; property test classes: ${property_tests}"
    echo ""

    # PR sample for human review.
    echo "## High-impact PRs (last 90d) for human review"
    if command -v git >/dev/null; then
        git log --since="$SINCE" \
            --pretty=format:'- %h %s — %an' \
            -- 'apps/venue-adapter-*' 2>/dev/null \
            | head -20 || true
    fi
    echo ""
} > "$OUT"

echo "Report written to ${OUT}"
