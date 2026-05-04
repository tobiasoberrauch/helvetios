# Trax APA Trade-Publication Interface

**Producer**: `apps/reporting-service/` + outbound FIX session in `apps/venue-adapter-marketaxess/`
**Protocol**: FIXT.1.1 + FIX 5.0 SP2 + EP228 + Trax custom tags (22683 etc.).
**Transport**: TLS-encrypted FIX session to MarketAxess Trax APA (primary) or CSV-SFTP (fallback for files ≥ 3GB).

## Submission flow

1. After every fill in scope (fixed income, structured finance, certain equities), the EMS hot-path emits a `tca.event.v1` with the deferral category (real-time / EOD / EOW).
2. Reporting service consumes the relevant fills and constructs a `TradeCaptureReport (35=AE)` per fill.
3. Sent on the outbound FIX session; Trax APA responds with `TradeCaptureReportAck (35=AR)` carrying the publication confirmation.
4. Trax-APA-specific custom tags populated:
   - 22683 = (per-publication routing hint)
   - others per the Trax-published spec.
5. Daily session reset at 23:00–23:05 GMT (Trax-published window).
6. CSV-SFTP fallback when payload size > 3GB (rare; only on heavy day-of-issue activity).
7. Publication ack reconciled against Trax-APA's published feed; mismatches raise Sev-3 alerts.

## Excerpt of FIX-message construction

```text
8=FIXT.1.1
9=...
35=AE
49=SWISSTMS
56=TRAX-APA
571=<TradeReportID = Fill.executionId>
487=0   (TradeReportTransType: New)
856=0   (TradeReportType: Submit)
55=<Symbol>
48=<SecurityID = ISIN>
22=4    (SecurityIDSource: ISIN)
207=<MIC>
32=<Quantity>
31=<Price>
75=<TradeDate>
60=<TransactTime — bizTime, UTC, microsecond>
22683=<TraxRoutingHint>
[EP228 publication-deferral fields]
10=...
```

## Idempotency

Trax APA enforces idempotency on `TradeReportID (571)`. Re-submitting the same `TradeReportID` returns the original ack; this is by design and does not result in duplicate publication.

## Reconciliation

A nightly batch job downloads the Trax APA publication feed and reconciles it against `cold.reporting.trax-apa-submission.v1`; mismatches raise Sev-3 alerts.
