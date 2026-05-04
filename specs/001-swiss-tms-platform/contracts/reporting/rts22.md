# MiFID-II RTS-22 — LSEG TRADEcho ARM Submission

**Producer**: `apps/reporting-service/`
**Format**: ESMA-published XML schema for transaction reports under MiFID-II RTS-22.
**Transport**: HTTPS POST to LSEG TRADEcho `/api/v1/transactions` (REST), authenticated by mTLS + OAuth2 client-credentials.

## Submission flow

1. Spring Batch job runs T+1 morning (typically 06:00 UTC).
2. Reads from `cold.exec.fill.v1` for the reporting day.
3. Maps to the ESMA RTS-22 schema. ARM-specific extensions handled in the LSEG TRADEcho dialect.
4. Validates against the ESMA XSD; rejects internally on validation failure (FR-027).
5. POSTs in batches of 1000 transaction reports per HTTP request.
6. Stores the LSEG-issued submission identifier and the per-record `Status: ACK` / `REJ` in `cold.reporting.rts22-submission.v1`.
7. Rejections trigger a Sev-3 alert and a follow-up job that retries with corrected payloads.

## Excerpt of mapping table

| RTS-22 field | Source | Notes |
|---|---|---|
| TransactionReferenceNumber | Fill.executionId | Globally unique |
| ExecutingEntity LEI | LegalEntity (the bank) | |
| InvestmentDecisionWithinFirm | Order.traderId → mappped to NID | National Identification |
| ExecutionWithinFirm | Order.traderId → mappped to NID | |
| BuyerSellerLEI | Client.LegalEntity.LEI | |
| TradingVenue MIC | Fill.venueId | |
| InstrumentClassification | Instrument.cfiCode | |
| TransactionDateTime | Fill.bizTime | UTC, microsecond |
| Quantity | Fill.quantity | |
| Price | Fill.price | |
| TradingCapacity | Order.tradingCapacity (DEAL / MTCH / AOTC) | |

## RTS-25 timestamp evidence

Every transaction report references the synchronised regulatory clock (RTS-25). The annual audit pack from `tools/ptp-audit-report/` is cross-referenced when FINMA / FCA examines the RTS-22 stream.

## ARM ack reconciliation

A separate Spring Batch job polls `cold.reporting.rts22-submission.v1` for unacknowledged records older than 24 hours and emits a Sev-3 alert. Re-submission requires compliance-officer approval in the UI.
