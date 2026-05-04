#!/usr/bin/env bash
#
# T086 — End-to-End Smoke Test.
#
# Submits an order via REST, polls until the order is FILLED, then
# checks the audit-chain hash linkage in Postgres.
#
# Voraussetzung: `tilt up` läuft (oms-service auf :8080, postgres mit
# Daten geladen). Wenn du nur den OMS-Container ohne Tilt willst:
#
#   docker compose -f compose.dev.yaml up -d postgres
#   ./gradlew :apps:oms-service:bootRun
#
# Im Erfolgsfall exits 0; sonst exits 1 mit Diagnose.

set -euo pipefail

OMS_URL="${OMS_URL:-http://localhost:8080}"
PG_URL="${PG_URL:-postgresql://swisstms:swisstms-dev@localhost:5432/swisstms}"
POLL_TIMEOUT_SEC="${POLL_TIMEOUT_SEC:-30}"

CL_ORD_ID="SMOKE-$(date +%s%N)"

echo "→ Submitting order ${CL_ORD_ID} to ${OMS_URL}/api/v1/orders"

RESPONSE=$(curl -fsS -X POST "${OMS_URL}/api/v1/orders" \
  -H "Content-Type: application/json" \
  -d @- <<EOF
{
  "clOrdId": "${CL_ORD_ID}",
  "instrumentId": { "isin": "CH0038863350", "mic": "XSWX" },
  "side": "BUY",
  "ordType": "LIMIT",
  "quantity": 100,
  "price": 99.50,
  "timeInForce": "DAY",
  "routingMode": "DMA",
  "preferredVenue": "XSWX"
}
EOF
)

ORDER_ID=$(printf '%s' "${RESPONSE}" | grep -oE '"orderId":"[^"]+"' | head -1 | cut -d'"' -f4)
if [[ -z "${ORDER_ID}" ]]; then
  echo "✗ No orderId in response: ${RESPONSE}"
  exit 1
fi
echo "→ orderId=${ORDER_ID}"

START=$(date +%s)
while true; do
  STATUS=$(curl -fsS "${OMS_URL}/api/v1/orders/${ORDER_ID}" | grep -oE '"status":"[^"]+"' | head -1 | cut -d'"' -f4)
  echo "  status=${STATUS}"
  if [[ "${STATUS}" == "FILLED" ]]; then
    break
  fi
  if [[ $(( $(date +%s) - START )) -ge ${POLL_TIMEOUT_SEC} ]]; then
    echo "✗ Order did not reach FILLED within ${POLL_TIMEOUT_SEC}s (last status=${STATUS})"
    exit 1
  fi
  sleep 1
done

echo "→ Verifying event-store hash chain"

if command -v psql >/dev/null 2>&1; then
  EVENT_COUNT=$(psql "${PG_URL}" -tAc \
    "SELECT count(*) FROM order_event WHERE order_id='${ORDER_ID}'")
  echo "  events in chain: ${EVENT_COUNT}"
  if [[ "${EVENT_COUNT}" -lt 4 ]]; then
    echo "✗ Expected ≥ 4 events (SUBMITTED + ACKED + PARTIAL_FILL + FILLED), got ${EVENT_COUNT}"
    exit 1
  fi
  HASH_OK=$(psql "${PG_URL}" -tAc "
    WITH ordered AS (
      SELECT seq, prev_hash, hash, lag(hash) OVER (ORDER BY seq) AS expected_prev
      FROM order_event WHERE order_id='${ORDER_ID}' ORDER BY seq
    )
    SELECT bool_and(seq=1 OR prev_hash=expected_prev) FROM ordered")
  if [[ "${HASH_OK}" != "t" ]]; then
    echo "✗ Hash chain broken for order ${ORDER_ID}"
    exit 1
  fi
  echo "  ✓ hash chain consistent"
else
  echo "  (skipping hash-chain check — psql not installed)"
fi

echo "✓ Smoke test passed."
