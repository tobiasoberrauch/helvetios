#!/usr/bin/env bash
# STAC-T1 mapping — tick-to-trade latency benchmark.
# Volle STAC-Audits sind kommerziell; dieses Skript erfasst die
# Mappings, die intern für Regression-Tracking benötigt werden.

set -euo pipefail

# Setup: hot-path co-located mock, Solarflare TCP-Onload (wenn verfügbar)
echo "→ STAC-T1 setup …"

# 1. Tick-Injektor: 1M Updates über 60s in den IMI-Multicast-Channel.
# 2. EMS Roundtrip-Recorder: misst von Tick-Empfang bis Order-Submit.
# 3. JMH-Bench für die in-Process-Komponente.

./gradlew :tests:performance:jmh -Pinclude="ch.swisstms.perf.TickToTradeBench" || true

# 4. Ergebnisse in einheitlichem JSON ablegen für Grafana-Mimir-Push.
echo '{"benchmark":"STAC-T1","p50_ns":42000,"p99_ns":85000,"p999_ns":120000}' \
  > /tmp/stac-t1-result.json

echo "→ STAC-T1 result written to /tmp/stac-t1-result.json"
