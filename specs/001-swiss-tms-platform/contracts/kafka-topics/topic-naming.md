# Kafka Topic Naming Convention

Format:

```
<tier>.<context>.<event>.v<n>
```

| Element | Allowed values | Meaning |
|---|---|---|
| `tier` | `hot` / `warm` / `cold` | Latency tier for the consumer (hot < 100µs is rare on Kafka — most hot-path goes via Aeron, but a few control-plane topics like `hot.killswitch.trip.v1` exist). |
| `context` | `oms` / `ems` / `exec` / `marketdata` / `clearing` / `recon` / `surveillance` / `reporting` / `entitlements` / `audit` / `region` / `dropcopy` / `tca` | Bounded context that owns the topic. |
| `event` | freeform, kebab-case | The event being published. |
| `v<n>` | `v1`, `v2`, … | Schema version. Every breaking change bumps the number; both versions coexist for at least one trading day. |

## Region partitioning

Each four-region active-active deployment runs an independent Kafka cluster per region. Cross-region topics are mirrored via MirrorMaker 2 with the convention:

- `<source-region>.<original-topic-name>` for replicated topics (e.g., `zh.cold.exec.fill.v1` in LD4 carries replicated Zurich-region fills).

The `region-router` (`apps/region-router/`) and `audit-service` consume the cross-region replicated topics for their global views; everything else operates on its local-region topic.

## Examples

```
hot.killswitch.trip.v1                  -- Aeron-prioritised low-volume control-plane
warm.dropcopy.six.v1                    -- SIX drop-copy stream
warm.recon.mismatch.v1                  -- reconciler mismatches
warm.entitlements.limit-update.v1       -- pre-trade-risk profile incremental updates
cold.oms.event.v1                       -- OMS event store outbox
cold.ems.algo-progress.v1               -- algo execution progress
cold.exec.fill.v1                       -- canonical Fill stream (post-recon)
cold.marketdata.l1.v1                   -- L1 ticks fan-out to cold-path consumers
cold.clearing.eurex-trade-capture.v1    -- Eurex trade-capture FIXML envelope wrapped in Avro
cold.clearing.margin-call.v1            -- CCP margin calls
cold.surveillance.alert.v1              -- abuse alerts
cold.surveillance.feedback.v1           -- analyst feedback for tuning
cold.reporting.rts22-submission.v1      -- RTS-22 submission records (incl. ack)
cold.reporting.finfrag-art39-submission.v1
cold.reporting.trax-apa-submission.v1
cold.reporting.emir-submission.v1
audit.command.v1                        -- hash-chained audit log of every command
region.handover.cutover.v1              -- follow-the-sun cutover events
tca.event.v1                            -- TCA records for RTS-28
```

## Schema registry

All topics use **Apache Avro** schemas registered with **Apicurio Registry** (deployed alongside Kafka in each region). The Avro schema files live in `contracts/avro/` in the source repo. Backward-compatibility mode: `BACKWARD_TRANSITIVE` (a v3 consumer can read v1 and v2 messages).

## Retention

| Topic prefix | Retention | Reason |
|---|---|---|
| `hot.*` | 7 days | Operational replay only |
| `warm.*` | 30 days | Operational + ad-hoc reconciliation |
| `cold.*` (most) | 1 year | Combined with Postgres event store and S3 WORM |
| `audit.command.v1` | 5 years | RTS-24 baseline; tiered to S3 WORM after 90 days |
| `cold.reporting.*` | 5 years (then S3 WORM forever) | Regulatory |
| `tca.event.v1` | 5 years | RTS-28 |
