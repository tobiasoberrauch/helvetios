# Kafka Topic Catalog

Authoritative list of every Kafka topic the platform owns. Schemas are versioned in `contracts/avro/`. Each row identifies the producer, consumers, partition key, and retention.

## Hot tier (control-plane only on Kafka; hot-path data goes via Aeron)

| Topic | Producer | Consumers | Partition key | Retention |
|---|---|---|---|---|
| `hot.killswitch.trip.v1` | `entitlements-service` | `ems-service`, `inbound-fix-acceptor`, `pretrade-risk-gateway` | `KillScope.id` | 7 days |

## Warm tier

| Topic | Producer | Consumers | Partition key | Retention |
|---|---|---|---|---|
| `warm.dropcopy.six.v1` | `venue-adapter-six` | `reconciler-service`, `audit-service` | `(SenderCompID, ClOrdID)` | 30 days |
| `warm.dropcopy.eurex.v1` | `venue-adapter-eurex` | `reconciler-service`, `audit-service` | `(SenderCompID, ClOrdID)` | 30 days |
| `warm.dropcopy.tradeweb.v1` | `venue-adapter-tradeweb` | `reconciler-service` | `(SenderCompID, ClOrdID)` | 30 days |
| `warm.dropcopy.marketaxess.v1` | `venue-adapter-marketaxess` | `reconciler-service` | `(SenderCompID, ClOrdID)` | 30 days |
| `warm.dropcopy.bidfx.v1` | `venue-adapter-bidfx` | `reconciler-service` | `(SenderCompID, ClOrdID)` | 30 days |
| `warm.dropcopy.bloomberg-emsx.v1` | `venue-adapter-bloomberg` | `reconciler-service` | `(EMSX_SEQUENCE)` | 30 days |
| `warm.recon.mismatch.v1` | `reconciler-service` | `ops-alerts`, `surveillance-service` | `OrderId` | 30 days |
| `warm.entitlements.limit-update.v1` | `entitlements-service` | `pretrade-risk-gateway` | `ClientId` | 30 days |
| `warm.intraday-position.v1` | `position-keeping` | `trader-ui`, `risk-aggregator` | `(accountId, instrumentId)` | 30 days |
| `warm.region.handover.signal.v1` | `region-router` | `inbound-fix-acceptor`, `oms-service` | constant (low-volume) | 30 days |

## Cold tier

| Topic | Producer | Consumers | Partition key | Retention |
|---|---|---|---|---|
| `cold.oms.event.v1` | `oms-service` (Outbox via Debezium) | `position-keeping`, `reporting-service`, `surveillance-service`, `audit-service` | `OrderId` | 1 year |
| `cold.ems.algo-progress.v1` | `ems-service` | `trader-ui`, `surveillance-service` | `ExecutionTaskId` | 1 year |
| `cold.exec.fill.v1` | `reconciler-service` (post-recon authoritative) | `position-keeping`, `clearing-adapter-eurex`, `reporting-service`, `surveillance-service` | `(OrderId)` | 1 year |
| `cold.marketdata.l1.v1` | `market-data-service` | `surveillance-service`, `tca-service`, `reporting-service` (best-execution evidence) | `(InstrumentId)` | 1 year (then ClickHouse for longer) |
| `cold.clearing.eurex-trade-capture.v1` | `clearing-adapter-eurex` | `position-keeping`, `reporting-service` | `ClearingTradeId` | 1 year |
| `cold.clearing.six-secom.v1` | `clearing-adapter-six` | `position-keeping`, `reporting-service` | `ClearingTradeId` | 1 year |
| `cold.clearing.margin-call.v1` | `clearing-adapter-*` | `risk-aggregator`, `treasury-service` | `(CCP, ClearingMember)` | 1 year |
| `cold.surveillance.alert.v1` | `surveillance-service` | `compliance-ui`, `audit-service` | `AlertId` | 1 year |
| `cold.surveillance.feedback.v1` | `compliance-ui` | `surveillance-service` (tuning) | `AlertId` | 1 year |
| `cold.reporting.rts22-submission.v1` | `reporting-service` | `audit-service`, `regulator-archive` | `TransactionReportId` | 5 years (then S3 WORM forever) |
| `cold.reporting.finfrag-art39-submission.v1` | `reporting-service` | `audit-service`, `regulator-archive` | `TransactionReportId` | 5 years |
| `cold.reporting.trax-apa-submission.v1` | `reporting-service` | `audit-service`, `regulator-archive` | `TransactionReportId` | 5 years |
| `cold.reporting.emir-submission.v1` | `reporting-service` | `audit-service`, `regulator-archive` | `TransactionReportId` | 5 years |
| `tca.event.v1` | `ems-service` (algo TCA hooks) + `venue-adapter-tradeweb` (AiEX hook) | `reporting-service` (RTS-28), `tca-research` | `(OrderId)` | 5 years |
| `audit.command.v1` | every service via `libs/audit-chain/` | `audit-service` | `region` | 5 years (S3 WORM after 90 days) |
| `region.handover.cutover.v1` | `region-router` | `audit-service`, `ops-dashboard` | constant | 1 year |
