# EMIR Reporting — DTCC GTR + REGIS-TR

**Producer**: `apps/reporting-service/`
**Format**: ESMA-published EMIR reporting schema (currently EMIR Refit ITS — bilateral CSV / XML).
**Transport**: HTTPS to DTCC GTR (`https://gtr.dtcc.com/api/v1/...`) and REGIS-TR (`https://regis-tr.com/...`).

## Submission flow

1. Spring Batch job runs T+1 (00:00 UTC + 30-minute buffer).
2. Reads from `cold.clearing.eurex-trade-capture.v1` and `cold.clearing.six-secom.v1` plus OTC trades from `cold.exec.fill.v1` filtered to OTC asset classes.
3. Maps to the EMIR Refit fields (UTI, USI, Action Type, Asset Class, Underlying Identification, Notional, etc.).
4. Validates against the published XSD.
5. Submits to BOTH DTCC GTR and REGIS-TR (dual-reporting mode is the conservative default for legal entities operating in both EU and UK).
6. Stores per-TR submission references in `cold.reporting.emir-submission.v1`.
7. Reconciles weekly against the TR's pair-reporting reports.

## Lifecycle reporting

EMIR mandates lifecycle event reports beyond initial submission. The reporting service emits:

- `NEWT` — new trade
- `MODI` — modification (terms changed)
- `CORR` — correction
- `EROR` — error correction
- `EARL` — early termination
- `TRAD` — trading-day-end snapshot

Each event consumes from the OMS / clearing event streams and emits a separate EMIR record.

## Pair-reporting reconciliation

Weekly job downloads the TR's pairing report and reconciles. Mismatched UTIs raise Sev-3 alerts and feed a `cold.reporting.emir-pairing-mismatch.v1` topic.
