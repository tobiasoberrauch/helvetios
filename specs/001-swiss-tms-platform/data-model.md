# Phase 1 Data Model: Swiss Trading & Market Support Platform

This document defines the aggregate roots, value objects, relationships, lifecycle state machines, and validation rules for each bounded context. Schema sketches are illustrative — the canonical schema lives in Flyway migrations under each service's `src/main/resources/db/migration/`.

The model strictly observes the hexagonal-with-venue-as-adapter principle — domain types here use **only** domain language (Order, Price, Quantity, Side, ...). FIX tags, SBE templates, BLPAPI identities live in adapters and codec libs.

---

## Conventions

- **Identifiers**: `OrderId`, `ExecutionId`, `ClientId`, `LegalEntityId`, etc. are typed value objects (not raw strings) implemented as Java `record`s wrapping a UUIDv7 (time-sortable) for surrogate keys, or a domain-natural identifier where applicable (`InstrumentId` = ISIN + MIC pair).
- **Money / quantity**: `Price` and `Quantity` are decimal-tick-aware value objects (BigDecimal under the hood, with explicit `Scale` and `TickSize`). Never use `double` or `float` for monetary values.
- **Timestamps**: every event carries `bizTime` (when it happened in the business sense, from the synchronised regulatory clock, microsecond precision per RTS-25) and `procTime` (when the system processed it). Domain code uses `RegulatoryClock` from `libs/time-sync/` to produce `bizTime`.
- **State machines**: implemented with Spring Statemachine; transitions are guarded; illegal transitions throw at compile-time where possible (sealed interfaces) or at runtime with structured error.
- **Audit**: every aggregate's command emits an `AuditEvent` to Kafka topic `audit.command.v1` with a SHA-256 hash chain via `libs/audit-chain/`.

---

## Bounded Context: Reference Data

Service: `apps/reference-data-service/` (Python/FastAPI).

### Aggregate: Instrument

```text
Instrument {
  InstrumentId          -- (ISIN, MIC) composite identifier
  symbol                -- venue-displayed symbol per venue
  assetClass            -- EQUITY | LISTED_DERIVATIVE | OTC_DERIVATIVE | FIXED_INCOME | FX | CRYPTO
  cfiCode               -- ISO 10962 CFI code
  isinCheckDigit        -- validated at write
  currency              -- ISO 4217
  tickSize              -- per-venue overridable
  lotSize               -- minimum tradeable size
  listedVenues          -- Set<MIC>
  primaryVenue          -- MIC
  status                -- ACTIVE | SUSPENDED | DELISTED
  lifecycleStart        -- date
  lifecycleEnd          -- date | null
  lei                   -- Issuer LEI (for fixed income)
  parentInstrumentId    -- nullable, for derivative→underlying chain
  productPermissions    -- Set<PE-Code> for DACS / Bloomberg entitlement matching
}
```

Validation rules:
- ISIN format `[A-Z]{2}[A-Z0-9]{9}[0-9]` and check-digit valid.
- `tickSize > 0`, `lotSize > 0`.
- `assetClass` consistent with `cfiCode` (e.g., CFI starting `E*` ⇒ `EQUITY`).
- For fixed income, `lei` of the issuer required.

Postgres schema sketch:

```sql
CREATE TABLE instrument (
  isin           CHAR(12) NOT NULL,
  primary_mic    CHAR(4)  NOT NULL,
  symbol         TEXT NOT NULL,
  asset_class    asset_class_enum NOT NULL,
  cfi_code       CHAR(6) NOT NULL,
  currency       CHAR(3) NOT NULL,
  tick_size      NUMERIC(18,8) NOT NULL,
  lot_size       NUMERIC(18,8) NOT NULL,
  listed_micss   CHAR(4)[] NOT NULL,
  status         instrument_status_enum NOT NULL,
  lifecycle_start DATE NOT NULL,
  lifecycle_end   DATE,
  lei            CHAR(20),
  parent_isin    CHAR(12),
  parent_primary_mic CHAR(4),
  pe_codes       INTEGER[] NOT NULL DEFAULT '{}',
  PRIMARY KEY (isin, primary_mic)
);
```

### Aggregate: LegalEntity

```text
LegalEntity {
  LegalEntityId  -- LEI (20 char)
  legalName
  jurisdiction
  entityType     -- BANK | BROKER | CLIENT | CCP | TR | ARM
  parentLei      -- nullable
}
```

### Aggregate: Calendar

```text
Calendar {
  CalendarId
  region          -- e.g., CH, GB, US, JP
  holidays        -- Set<LocalDate>
  earlyCloseDays  -- Map<LocalDate, LocalTime>
}
```

---

## Bounded Context: Inbound Client (Sell-Side)

Service: `apps/inbound-fix-acceptor/` (Java/Artio).

### Aggregate: Client

```text
Client {
  ClientId
  legalEntityId            -- ref LegalEntity
  status                   -- ONBOARDED | SUSPENDED | OFFBOARDED
  preferredRegion          -- ZH | LD4 | NY4 | TY3
  fallbackRegion
  pretradeRiskProfile      -- ref PretradeRiskProfile
  permittedAssetClasses    -- Set<AssetClass>
  permittedInstruments     -- Set<InstrumentId>  (allowlist) OR
  restrictedInstruments    -- Set<InstrumentId>  (blocklist)
  routingMode              -- DMA_ONLY | DMA_AND_ALGO | DMA_AND_ALGO_AND_CARE
  defaultAlgo              -- nullable
}
```

### Aggregate: FixSession

```text
FixSession {
  SessionId               -- (SenderCompID, TargetCompID, sessionQualifier)
  clientId
  fixVersion              -- FIX_4_4 | FIX_5_0_SP2_FIXT_1_1
  inboundIp               -- whitelisted source(s)
  mtlsClientCertFingerprint
  state                   -- LOGGED_OUT | LOGGED_ON | RESYNCING | DISCONNECTED
  nextSenderSeq
  nextTargetSeq
  lastLogon
  throttle                -- per-second + max in-flight
}
```

State transitions:
```
LOGGED_OUT --logon--> LOGGED_ON
LOGGED_ON --gap_detected--> RESYNCING
RESYNCING --resend_complete--> LOGGED_ON
LOGGED_ON --logout/disconnect--> LOGGED_OUT
* --tcp_reset--> DISCONNECTED
DISCONNECTED --reconnect--> LOGGED_OUT
```

### Aggregate: PretradeRiskProfile

```text
PretradeRiskProfile {
  ProfileId
  clientId
  fatFingerNotional        -- per single order, currency-converted
  fatFingerQuantity        -- per single order
  maxOrderSizeNotional
  ordersPerSecondLimit
  inFlightOrdersLimit
  notionalDailyLimit       -- per asset class
  restrictedInstruments    -- denylist (overrides Client.permittedInstruments allowlist)
  killSwitchState          -- ENABLED | DISABLED (denormalised for hot-path cache)
  version                  -- monotonically increasing for cache invalidation
}
```

These profiles are loaded into off-heap Agrona maps in `apps/pretrade-risk-gateway/` at startup and incrementally updated via Kafka topic `warm.entitlements.limit-update.v1`.

---

## Bounded Context: Order Management

Service: `apps/oms-service/` (Java/Spring Boot).

### Aggregate Root: Order

```text
Order {
  OrderId                 -- internal UUIDv7
  clOrdId                 -- client's identifier (FIX Tag 11)
  origClOrdId             -- for cancel/replace chain
  clientId
  traderId                -- for care orders / sell-side flow attribution
  region                  -- which region owns this order (follow-the-sun)
  instrumentId
  side                    -- BUY | SELL | SELL_SHORT
  quantity                -- Quantity
  price                   -- Price (nullable for market orders)
  ordType                 -- MARKET | LIMIT | STOP | STOP_LIMIT | FUNARI | MOO | LOO | ...
  timeInForce             -- DAY | IOC | FOK | GTC | GTD | OPG
  expireTime              -- for GTD
  routingMode             -- DMA | ALGO_WHEEL | CARE
  algoStrategy            -- nullable: VWAP | TWAP | POV | IS | ...
  algoParameters          -- Map<String,String>
  ordStatus               -- state machine (see below)
  cumQty                  -- sum of fills so far
  leavesQty               -- remaining
  avgPx                   -- average fill price
  submittedAtBiz          -- regulatory timestamp
  submittedAtProc
  lastUpdatedAtBiz
  preferredVenue          -- nullable
  executionVenue          -- after first fill
  executionId             -- last venue exec id
  parentOrderId           -- for parent-child algo chains
  childOrderIds           -- Set<OrderId>
}
```

OrdStatus state machine:

```
NEW --ack--> ACKNOWLEDGED
ACKNOWLEDGED --partial_fill--> PARTIALLY_FILLED
ACKNOWLEDGED --full_fill--> FILLED
PARTIALLY_FILLED --partial_fill--> PARTIALLY_FILLED
PARTIALLY_FILLED --full_fill--> FILLED
ACKNOWLEDGED --cancel_request_acked--> PENDING_CANCEL
PARTIALLY_FILLED --cancel_request_acked--> PENDING_CANCEL
PENDING_CANCEL --cancel_confirmed--> CANCELLED
ACKNOWLEDGED --replace_request_acked--> PENDING_REPLACE
PENDING_REPLACE --replace_confirmed--> ACKNOWLEDGED
NEW --reject--> REJECTED
ACKNOWLEDGED --reject--> REJECTED  (e.g., venue-side error after ack)
FILLED --trade_bust--> TRADE_BUSTED
* --expire--> EXPIRED  (only when TimeInForce conditions met)
```

Invariants:
- After `FILLED`, only `TRADE_BUSTED` transitions are legal (FR-002).
- `cumQty + leavesQty == originalQty` at all times (excluding amendments).
- `avgPx` recomputed on every fill from the cumulative VWAP of fills.
- `executionVenue` becomes immutable once set on first fill.

Postgres schema sketch:

```sql
CREATE TABLE order_aggregate (
  order_id           UUID PRIMARY KEY,
  cl_ord_id          TEXT NOT NULL,
  orig_cl_ord_id     TEXT,
  client_id          UUID NOT NULL,
  trader_id          UUID,
  region             region_enum NOT NULL,
  instrument_isin    CHAR(12) NOT NULL,
  instrument_mic     CHAR(4)  NOT NULL,
  side               side_enum NOT NULL,
  ord_type           ord_type_enum NOT NULL,
  time_in_force      tif_enum NOT NULL,
  expire_time        TIMESTAMPTZ,
  quantity           NUMERIC(18,8) NOT NULL CHECK (quantity > 0),
  price              NUMERIC(18,8) CHECK (price > 0),
  routing_mode       routing_mode_enum NOT NULL,
  algo_strategy      algo_strategy_enum,
  algo_parameters    JSONB,
  ord_status         ord_status_enum NOT NULL,
  cum_qty            NUMERIC(18,8) NOT NULL DEFAULT 0,
  leaves_qty         NUMERIC(18,8) NOT NULL,
  avg_px             NUMERIC(18,8),
  submitted_at_biz   TIMESTAMPTZ NOT NULL,
  submitted_at_proc  TIMESTAMPTZ NOT NULL,
  last_updated_at    TIMESTAMPTZ NOT NULL,
  preferred_venue    CHAR(4),
  execution_venue    CHAR(4),
  parent_order_id    UUID REFERENCES order_aggregate(order_id),
  CONSTRAINT cl_ord_id_per_session_unique UNIQUE (client_id, cl_ord_id),
  CONSTRAINT cum_leaves_consistency CHECK (cum_qty + leaves_qty <= quantity * 10)  -- amendments may inflate this; precise check in app
);

CREATE INDEX ix_order_client_status ON order_aggregate (client_id, ord_status);
CREATE INDEX ix_order_region_status ON order_aggregate (region, ord_status);
CREATE INDEX ix_order_parent ON order_aggregate (parent_order_id);
```

### Aggregate: Allocation

```text
Allocation {
  AllocationId
  orderId
  accountId
  quantity
  status     -- PENDING | CONFIRMED | REJECTED
}
```

### Outbox / Event Store integration

Every state transition writes:
1. An update to `order_aggregate` (current state).
2. An immutable row to `order_event` (event sourced log).
3. A row to `outbox` (Debezium-driven publish to Kafka topic `cold.oms.event.v1`).
4. An audit chain entry via `libs/audit-chain/` to `audit.command.v1`.

```sql
CREATE TABLE order_event (
  event_id      UUID PRIMARY KEY,
  order_id      UUID NOT NULL REFERENCES order_aggregate(order_id),
  seq           BIGINT NOT NULL,                -- monotonic per order
  event_type    TEXT NOT NULL,                   -- 'OrderSubmitted' | 'OrderAcked' | ...
  payload       JSONB NOT NULL,                  -- typed event payload
  biz_time      TIMESTAMPTZ NOT NULL,
  proc_time     TIMESTAMPTZ NOT NULL,
  prev_hash     BYTEA NOT NULL,                  -- SHA-256 of previous event in chain
  hash          BYTEA NOT NULL,                  -- SHA-256(prev_hash || canonical(payload))
  CONSTRAINT ord_event_seq UNIQUE (order_id, seq)
);
```

---

## Bounded Context: Execution

Service: `apps/ems-service/` (Java/Aeron Cluster + Disruptor).

### Aggregate: ExecutionTask

Represents the lifecycle of an algo / SOR execution slice.

```text
ExecutionTask {
  ExecutionTaskId
  parentOrderId           -- the originating Order
  algoStrategy
  algoParameters
  startTime               -- biz time of START
  endTime                 -- nullable until DONE
  status                  -- PENDING | RUNNING | PAUSED | DONE | CANCELLED | FAILED
  childOrderIds           -- Set<OrderId>
  fillsToDate             -- Quantity
}
```

### Aggregate: Fill (Execution / ExecutionReport)

```text
Fill {
  FillId                  -- internal UUIDv7
  executionId             -- venue ExecID (from FIX 17)
  orderId                 -- ref Order
  parentOrderId           -- if from algo child
  venueId                 -- MIC
  quantity
  price
  side
  liquidityIndicator      -- ADD | REMOVE | CROSSED | AUCTION
  bizTime
  procTime
  ddpComplete             -- bool, set true once drop-copy reconciled
  source                  -- VENUE_SESSION | DROP_COPY | RECONCILED_BOTH
}
```

Reconciliation: `Fill` rows arriving from the OMS execution stream and the drop-copy stream are joined on `(SenderCompID, ClOrdID, ExecID)` in `apps/reconciler-service/`. Disagreements emit a `cold.recon.mismatch.v1` event; drop-copy wins. (FR-011, FR-012, FR-013.)

---

## Bounded Context: Market Data

Service: `apps/market-data-service/` (Java/Aeron).

### Aggregate: InstrumentBook

In-memory aggregate; not persisted at L1/L2 — only the hot tier QuestDB receives downsampled / OHLCV writes. ClickHouse receives long-horizon historical writes.

```text
InstrumentBook {
  instrumentId
  bestBid                 -- (price, qty, count)
  bestAsk
  topNLevels              -- ordered list per side
  lastTrade               -- (price, qty, biz_time)
  vwapDayToDate
  cumulativeVolume
  lastSequence            -- per source, gap detection
}
```

### Subscription

```text
Subscription {
  SubscriptionId
  subscriberId           -- TraderId or downstream service id
  instrumentId
  level                  -- L1_TOP_OF_BOOK | L2_DEPTH | L3_FULL_BOOK
  source                 -- which adapter delivers (REFINITIV_EMA | BLOOMBERG_BPIPE | SIX_IMI | …)
  state                  -- REQUESTED | ENTITLED | STREAMING | STOPPED | DENIED
  entitlementChecks      -- last result + timestamp
}
```

State transitions:
```
REQUESTED --entitled--> ENTITLED
ENTITLED --first_tick--> STREAMING
REQUESTED --not_entitled--> DENIED
* --revoked--> STOPPED
* --upstream_disconnect--> STOPPED
```

Tick storage (QuestDB):

```sql
CREATE TABLE tick_l1 (
  ts          TIMESTAMP,
  isin        SYMBOL CAPACITY 100000 CACHE,
  mic         SYMBOL CAPACITY 200 CACHE,
  bid_px      DOUBLE,
  bid_qty     DOUBLE,
  ask_px      DOUBLE,
  ask_qty     DOUBLE,
  last_px     DOUBLE,
  last_qty    DOUBLE,
  src         SYMBOL CAPACITY 50 CACHE
) timestamp(ts) PARTITION BY DAY WAL;
```

ClickHouse schema mirrors but partitioned `BY toYYYYMM(ts)` with `MergeTree` engine and longer retention.

---

## Bounded Context: Position Keeping

Service: `apps/position-keeping/` (Java; subscribes to OMS `cold.exec.fill.v1`).

### Aggregate: Position

```text
Position {
  PositionKey            -- (accountId, instrumentId, valueDate)
  netQuantity
  averageCost
  realisedPnl
  unrealisedPnl
  lastFillId             -- idempotency
  lastUpdatedBiz
}
```

Updates are idempotent on `lastFillId` to handle Kafka redelivery.

---

## Bounded Context: Clearing & Settlement

Services: `apps/clearing-adapter-eurex/`, `apps/clearing-adapter-six/`, `apps/clearing-adapter-otcc/`.

### Aggregate: ClearingTrade

```text
ClearingTrade {
  ClearingTradeId
  fillId                 -- ref OMS Fill
  ccp                    -- EUREX | SIX_X_CLEAR | OTCC
  clearingMember
  novationStatus         -- PENDING | NOVATED | REJECTED
  initialMargin
  variationMargin
  netting                -- ref nettingSet
  settlementDate
  settlementInstruction  -- ISO 20022 sese.023 ref (for SECOM) or FpML/FIXML (for Eurex)
}
```

Lifecycle (Saga, in Temporal):

```
SUBMITTED --capture_ack--> PENDING_NOVATION
PENDING_NOVATION --novation_confirmed--> NOVATED
PENDING_NOVATION --novation_rejected--> REJECTED
NOVATED --margin_call--> MARGIN_CALL_OPEN
MARGIN_CALL_OPEN --vm_paid--> NOVATED
NOVATED --settled--> SETTLED
```

### Aggregate: MarginCall

```text
MarginCall {
  MarginCallId
  ccp
  clearingMember
  type                  -- IM | VM | DEFAULT_FUND_TOP_UP
  amount
  currency
  callTime
  payByTime
  status                -- OPEN | PAID | DISPUTED | CASCADED
}
```

CCP default waterfall (defensive documentation only — not directly modelled): `IM → DF → SITG → mutualised DF → assessments → VMGH`. Handling lives in `docs/architecture/clearing.md`.

---

## Bounded Context: Reporting

Service: `apps/reporting-service/` (Java/Spring Batch).

### Aggregate: TransactionReport

```text
TransactionReport {
  TransactionReportId
  reportType            -- FINFRAG_ART39 | RTS22 | TRAX_APA | EMIR_DTCC | EMIR_REGIS
  reportingDate
  reportingScope        -- which trades / which legal entity
  status                -- PENDING | GENERATED | VALIDATED | SUBMITTED | ACKNOWLEDGED | REJECTED
  payloadXmlSha256      -- canonicalised XML hash for tamper evidence
  submissionRef         -- regulator's ack ID
  submittedAt
  acknowledgedAt
}
```

### Aggregate: AbuseAlert (Surveillance)

```text
AbuseAlert {
  AlertId
  alertType             -- LAYERING | SPOOFING | INSIDER_FRONT_RUNNING | ...
  severity              -- LOW | MEDIUM | HIGH | CRITICAL
  traderId
  instrumentId
  windowStart
  windowEnd
  evidenceEvents        -- ordered list of OrderEvent / ExecutionEvent ids
  status                -- OPEN | INVESTIGATING | TRUE_POSITIVE | FALSE_POSITIVE | CLOSED
  analystId             -- who is investigating / closed
  feedbackComment
}
```

---

## Bounded Context: Entitlements & Kill-Switch

Service: `apps/entitlements-service/` (Java).

### Aggregate: Entitlement

```text
Entitlement {
  EntitlementId
  subjectType           -- USER | APPLICATION
  subjectId             -- TraderId / AppId
  productPermission     -- DACS PE-Code or Bloomberg seatType:UUID:authID
  scope                 -- which instruments / asset classes
  source                -- DACS | BLOOMBERG_EMRS | INTERNAL
  validFrom
  validUntil
  state                 -- GRANTED | REVOKED | EXPIRED
}
```

### Aggregate: KillZone

```text
KillZone {
  KillZoneId
  scopeType             -- TRADER | STRATEGY | DESK | CLIENT
  scopeId
  state                 -- ARMED | TRIPPED | RESET
  trippedAt
  trippedBy             -- userId who pulled the switch
  reason
}
```

Kill-switch invariants:
- Tripping cancels every open order in scope (immediate, hot-path eviction).
- New orders for a tripped scope are rejected at the inbound risk gateway with a defined FIX BMR / Reject.
- Resetting requires a different operator from the tripper (4-eyes principle).

---

## Bounded Context: Audit

Service: `apps/audit-service/` (Java).

### Aggregate: AuditEvent

```text
AuditEvent {
  AuditEventId
  seq                  -- global monotonic, per-region; cross-region reconciliation by seq+region
  region
  actorType            -- USER | SERVICE | EXTERNAL
  actorId
  action               -- e.g., 'order.submit' | 'killswitch.trip' | 'entitlement.revoke'
  targetType
  targetId
  payload              -- canonical JSON of the command / change
  bizTime
  procTime
  prevHash             -- SHA-256 of previous AuditEvent in same region
  hash                 -- SHA-256(prevHash || canonical(payload))
  worMirror            -- s3://… reference once archived
}
```

Hash chain validation runs daily; any mismatch is treated as a Sev-1 incident.

---

## Multi-region considerations

Every aggregate that exists per-region (Order, ExecutionTask, AuditEvent) carries a `region` field. The `region-router` (`apps/region-router/`) tags inbound orders with the target region. Cross-region lookup goes through Aurora Global Database read replicas; cross-region writes are **not** permitted on the OMS hot path — instead, follow-the-sun handover replicates the in-flight state via Kafka MirrorMaker 2 to the new region's OMS at the configured cutover time.

Audit chain is per-region (separate hash chains per region); the regulator audit pack joins the four regional chains and verifies each independently, then concatenates by `bizTime`.

---

## Cross-cutting validation rules driven by FRs

| FR | Rule | Where enforced |
|---|---|---|
| FR-002 | OrdStatus transitions must be legal | Spring Statemachine in `libs/domain-model/order/` |
| FR-003 | Every transition writes an event | `OrderEventListener` in `apps/oms-service/` |
| FR-005c | Every order passes pre-trade risk before OMS | `apps/pretrade-risk-gateway/` |
| FR-005f | Inbound throttle limits enforced | `apps/inbound-fix-acceptor/` per-session counters |
| FR-011 | Drop-copy treated as authoritative | `apps/reconciler-service/` mismatch resolution |
| FR-021 | Entitlement check before delivery | `apps/market-data-service/` subscription gate |
| FR-022 | Kill-switch cancels all in-scope orders | `apps/entitlements-service/` + Kafka `hot.killswitch.trip.v1` consumed by EMS |
| FR-027 | Reports validated before submission | `apps/reporting-service/` `XmlValidator` step |
| FR-031 | Trading-server clocks ≤ 100µs UTC | `libs/time-sync/` + `ptp4l/phc2sys` infra |
| FR-034 | Hash-chained audit log | `libs/audit-chain/` |

---

## Cardinality summary

| Aggregate | Approx. peak cardinality | Rationale |
|---|---:|---|
| Instrument | 5M | global multi-asset universe |
| LegalEntity | 200k | clients + counterparties + venues + CCPs |
| Calendar | 50 | per-region |
| Client | 5,000 | sell-side prime broker scale |
| FixSession | 10,000 | up to 200 concurrent inbound (SC-016) + outbound venue sessions |
| Order | 10M / day, 100M open at any time during steady state | SC-013 |
| Allocation | 30M / day | typical 3 allocs per order |
| Fill | 20M / day | typical 2 fills per order |
| ExecutionTask | 100k / day | algos and SOR slices |
| InstrumentBook | 5M | one per Instrument |
| Subscription | 50M | 10k traders × ~5k subs each (cap-controlled) |
| Position | 10M | account × instrument × value-date |
| ClearingTrade | 5M / day | listed-derivative side |
| MarginCall | 10k / day | typical CCP margin-call frequency |
| TransactionReport | 1k / day | regulatory daily batch outputs |
| AbuseAlert | 1k / day | tunable |
| Entitlement | 5M | all users × all products |
| KillZone | 1k | small static set |
| AuditEvent | 100M / day | every command logged |
