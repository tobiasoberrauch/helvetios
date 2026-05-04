# FinfraG Art. 39 — SIX Trade Repository Submission

**Producer**: `apps/reporting-service/`
**Format**: TRI-XML (Swiss format) or ESMA-format XML (configurable per legal entity).
**Transport**: SFTP to `tr.six-group.com`, authenticated by SSH public key (key in OpenBao, fingerprint registered in the SIX TR member portal).

## Submission flow

1. Spring Batch job triggers daily at 22:00 UTC (configurable per region).
2. Job reads from `cold.exec.fill.v1` for the reporting day, joins with `cold.clearing.eurex-trade-capture.v1` and `cold.clearing.six-secom.v1`, applies the FinfraG Art. 39 mapping rules.
3. XML produced is canonicalised (XMLDsig C14N) and SHA-256 hashed; the hash is stored in the `TransactionReport` aggregate.
4. XML is validated against the SIX-TR-published XSD; failures abort the submission with a Sev-2 alert.
5. SFTP upload to the SIX TR inbound folder. The submission file name follows the SIX-TR convention: `<LEI>_<reportDate>_<batchSeq>.xml`.
6. SIX TR returns an `ack.xml` to the SFTP outbound folder; a separate Spring Batch job polls for the ack and updates the `TransactionReport.status` to `ACKNOWLEDGED` or `REJECTED`.
7. Both submission and ack are persisted to S3 WORM under `s3://swisstms-regulatory/finfrag-art39/<year>/<month>/<reportingDay>/`.

## Field mapping (excerpt)

| FinfraG field | Source | Notes |
|---|---|---|
| ReportingFirm.LEI | LegalEntity (the bank) | |
| Counterparty.LEI | Client.LegalEntity.LEI | |
| Trade.ID | Fill.executionId | |
| Trade.Timestamp | Fill.bizTime | UTC, microsecond precision |
| Trade.InstrumentISIN | Fill.instrumentId.isin | |
| Trade.Quantity | Fill.quantity | absolute value |
| Trade.Price | Fill.price | |
| Trade.Currency | Fill.instrument.currency | |
| Trade.Side | Fill.side | mapping table to FinfraG codes |
| Trade.TradingVenue | Fill.venueId (MIC) | |
| Trade.AssetClass | Instrument.assetClass | mapped to ESMA values |
| Submission.SequenceNumber | monotonically increasing per legal entity | |

## Idempotency

Re-running the same reporting day is a no-op (returns the existing `TransactionReport` and its previously-acknowledged status). Forced regeneration requires `?regenerate=true` on the `/reports/FINFRAG_ART39/run` endpoint and writes a new audit-chain entry citing the override reason.

## Bulk size limit

If the generated payload exceeds the SIX TR file size cap (typically 1 GB), the job splits into multiple files (`_part-001`, `_part-002`, …) and submits each, retaining a single logical `TransactionReport` covering all parts.
