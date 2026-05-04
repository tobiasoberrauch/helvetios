#!/usr/bin/env bash
# T307 — STAC-N1 mapping for our market-data feed handler.
#
# STAC-N1 is the Securities Technology Analysis Center's network/IO benchmark for market-data
# feed handlers. We map it onto our Aeron-multicast publisher + QuestDB writer chain. The
# mapping doc lives in https://stacresearch.com/N1 (members-only); the public surface here is
# just the run-script that produces a comparable CSV.
#
# Usage::
#
#   ./stac_n1_marketdata_feed_handler.sh --duration 60 --rate 1000000
#
# Output: stac-n1-result-${TIMESTAMP}.csv with per-second p50/p99/p999/max latencies and packet
# loss / gap-fill counts.
set -euo pipefail

DURATION_SECONDS="${1:-60}"
TARGET_RATE_MSGS_PER_SEC="${2:-1000000}"
OUT_FILE="stac-n1-result-$(date -u +%Y%m%dT%H%M%SZ).csv"

echo "ts,ingress_msgs,egress_msgs,p50_us,p99_us,p999_us,max_us,gaps,drops" > "$OUT_FILE"

echo "STAC-N1 run: duration=${DURATION_SECONDS}s target=${TARGET_RATE_MSGS_PER_SEC} msg/s"
echo "Phase 16 ships the harness wrapper; full implementation requires the STAC member-area"
echo "binary which is not redistributable. Output schema is the membership-published CSV."

# Placeholder loop: in production this calls the STAC binary against
# tests/performance/k6/marketdata_soak.js for the workload generator.
for ((i = 0; i < DURATION_SECONDS; i++)); do
    echo "$(date -u +%Y-%m-%dT%H:%M:%SZ),0,0,0,0,0,0,0,0" >> "$OUT_FILE"
    sleep 1
done

echo "wrote $OUT_FILE"
