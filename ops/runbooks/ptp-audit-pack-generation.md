# Runbook — RTS-25 PTP Audit Pack Generation (T218)

**Owner:** Market Support / IT-Ops
**Cadence:** Annual (FINMA submission window: end of Q1 each year)
**Last revised:** 2026-05-04

## Why

RTS-25 (MiFID II Art. 50) requires every trading venue / SI / MIC participant to demonstrate
clock synchronisation to UTC within ≤100 µs for trading systems. The audit pack documents the
divergence statistics for each PTP-disciplined server and is the artefact FINMA / SIX inspectors
ask for. The pack MUST be tamper-evident; we sign it with cosign so any byte modification
invalidates the signature.

## Inputs

* PTP daily logs from `ptp4l` and `phc2sys` (deployed by the playbooks below).
* Logs are shipped via Vector (`ops/loki/vector/ptp-pipeline.yml`) into OpenSearch index pattern
  `ptp-logs-*`.
* The reporting period is whatever range FINMA requested (typically a full calendar year).

## Procedure

```bash
# 1. Build the tool (Go workspaces — runs from repo root).
task build:go        # or: go build ./tools/ptp-audit-report/...

# 2. Pull the logs straight from OpenSearch and render a signed PDF.
./tools/ptp-audit-report/ptp-audit-report \
    --opensearch https://opensearch.internal:9200 \
    --index "ptp-logs-*" \
    --os-user audit \
    --os-pass "$OPENSEARCH_AUDIT_PW" \
    --period "2026-Q2" \
    --format pdf \
    --out audit-2026-Q2.pdf

# 3. Sign the PDF (cosign — keyless OIDC against our internal Fulcio).
cosign sign-blob \
    --key cosign://swisstms-audit \
    --output-signature audit-2026-Q2.pdf.sig \
    audit-2026-Q2.pdf

# 4. Verify before submission.
cosign verify-blob \
    --key cosign://swisstms-audit \
    --signature audit-2026-Q2.pdf.sig \
    audit-2026-Q2.pdf
```

## Submission

* Upload `audit-2026-Q2.pdf` + `audit-2026-Q2.pdf.sig` to the FINMA portal
  ({{ submission_url }}); attach the cosign-public-key bundle.
* Filing reference goes into the Compliance ledger (Linear project COMP-RTS25).

## Failure handling

| Symptom | Likely cause | First action |
|---|---|---|
| `no samples loaded` | OpenSearch index empty for the window | Check Vector pipeline; re-run `infra/ansible/playbooks/ptp-clients.yml` |
| Tool exits non-zero with `VIOLATION` warnings | One or more servers > 100 µs from UTC | Investigate the boundary clock, escalate to Network |
| `cosign verify-blob` fails | Signature mismatch — likely byte-level edit | Regenerate from scratch; do NOT attempt to patch |

## See also

* [`docs/decisions/0007-ptp-rts25.md`](../decisions/0007-ptp-rts25.md) — architectural decision
* [`ops/prometheus/alerts/ptp-divergence.yml`](../../ops/prometheus/alerts/ptp-divergence.yml) — live alert
* [`infra/ansible/playbooks/ptp-grandmaster.yml`](../../infra/ansible/playbooks/ptp-grandmaster.yml) — Meinberg LANTIME setup
