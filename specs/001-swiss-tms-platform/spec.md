# Feature Specification: Swiss Trading & Market Support Platform (Reference Mono-Repo)

**Feature Branch**: `001-swiss-tms-platform`
**Created**: 2026-05-03
**Status**: Draft
**Input**: User description: "Trading & Market Support Engineer Reference Mono-Repo — Spezifikation für eine Schweizer Bank in Basel" (Blueprint v1.0, Mai 2026)

## Overview

A reference trading and market support platform that demonstrates, end-to-end, how a Swiss bank in Basel would connect to the venues, clearing houses, market-data vendors, and regulators relevant to its business. The platform serves three audiences simultaneously:

1. **Learning artifact** — each integration technology (FIX, FIXML, FpML, SBE, AMQP 1.0, market-data SDKs, PTP time-sync, etc.) is anchored at a concrete location in the code tree.
2. **Portfolio piece** — the architecture mirrors the publicly-documented stack of UBS, RBC Capital Markets, HSBC Equities, Man Group, and SIX Interbank Clearing so a hiring manager recognises it as professionally credible within 90 seconds.
3. **Runnable system** — `tilt up` brings a local order roundtrip up in under ten minutes against in-process venue mocks.

The platform is structured as a polyglot mono-repo organised around bounded contexts (Order Management, Execution, Market Data, Clearing & Settlement, Reporting & Surveillance, Entitlements, Reference Data) with venues, clearing houses, and data vendors implemented as interchangeable outbound adapters behind a shared port interface.

## Clarifications

### Session 2026-05-03

- Q: Should the implementation plan target the whole platform, only Phase 1, or a layered approach? → A: Whole platform — architecture plan and task-level breakdown for all 50 FRs across all six phases in a single `/speckit.plan` invocation.
- Q: How deep should non-functional concerns (retention, ARM submissions, PTP, secrets, mTLS) be implemented end-to-end vs stubbed? → A: Production-shadow-grade — full hardening, hardware PTP grandmaster, real WORM with retention lock, every external integration real or against a vendor sandbox. Local-dev environment still uses mocks; the production-shadow environment is fully hardened.
- Q: What is the throughput / sizing target for the platform? → A: Sell-side prime broker scale — up to 10,000 concurrent traders, up to 10M orders/day, up to 50M market-data ticks/sec across all subscriptions. Sustained sell-side workload, not Swiss-private-bank scale.
- Q: How do external clients submit orders to the platform at sell-side scale? → A: FIX-as-server primary — the platform exposes inbound FIX 4.4 / 5.0 SP2 acceptor sessions per client, with per-client `SenderCompID` whitelisting, a pre-trade risk gateway in the hot path, and DMA / care-order / algo-wheel routing options behind a single inbound port.
- Q: What is the regional deployment topology? → A: Four-region full follow-the-sun — Zurich (ZH4/ZH5), London (LD4), New York (NY4), Tokyo (TY3) all active-active, with cross-region replication of all stateful tiers and book-of-business handover with the trading day. Aeron Cluster is per-region (Raft RTT bounds), Kafka/Postgres/cold-path replicate cross-region.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - End-to-End Order Roundtrip Against First Venue (Priority: P1)

A buy-side trader at the bank submits an equity order through the trader UI for a Swiss blue-chip listed on SIX Swiss Exchange. The order flows through Order Management, is routed to the SIX venue adapter, is acknowledged, partially filled, then fully filled. Every state change is persisted to an append-only event store, the final fill is reconciled against an independent drop-copy stream, and the trader sees the executed position in real time.

**Why this priority**: This is the platform's "hello world." Without a working order roundtrip there is no trading platform, no portfolio demo, and no place to anchor every subsequent integration. The Phase 1 milestone of the roadmap explicitly requires this slice to be demonstrable end-to-end.

**Independent Test**: Run `tilt up`, submit a `NewOrderSingle` REST request, observe an `ExecutionReport` flowing back through the OMS, see the order persisted in the trade-state store, see the drop-copy reconciler confirm the fill, and observe the position update in the UI — all against the bundled SIX mock venue, with no external dependency.

**Acceptance Scenarios**:

1. **Given** the platform is running locally with the SIX mock venue, **When** a trader submits a valid limit order, **Then** the system returns an order acknowledgement within 200ms and the order appears in the trader's open-order list.
2. **Given** an order has been acknowledged, **When** the venue mock generates a partial fill followed by a final fill, **Then** the OMS reflects `PartiallyFilled` and then `Filled` status, and the cumulative quantity on the order matches the sum of the fills.
3. **Given** an order has been fully filled, **When** the drop-copy reconciler processes the independent execution stream, **Then** all fills match (by `(SenderCompID, ClOrdID, ExecID)`) without raising a reconciliation alert.
4. **Given** the system is restarted mid-trading-day, **When** it comes back up, **Then** open orders are reconstructed from the event store with correct state and sequence numbers resume without gaps.

---

### User Story 2 - Drop-Copy as Independent Source of Truth (Priority: P1)

A market-support engineer is paged at 3 a.m. because the OMS has been unreachable for 50 minutes during which the venue continued to fill orders. The engineer relies on the drop-copy stream — captured continuously and independently of the OMS — to reconstruct the trading state, reconcile against the recovered OMS, and confirm no fill was lost or duplicated.

**Why this priority**: This is the regulatory and operational pattern every experienced trading engineer recognises immediately. Drop-copy as source-of-truth is what FINMA audits expect and what a hiring manager will probe in the interview ("how would you handle a 50-minute outage with 12,000 fills?"). Without it the platform is not credible.

**Independent Test**: Force the OMS to stop, let the venue mock emit several hundred fills into the drop-copy stream, restart the OMS, and observe that the reconciler reports all fills accounted for and the order book is reconstructed without manual intervention.

**Acceptance Scenarios**:

1. **Given** the OMS is stopped while the venue continues to send fills, **When** the OMS is restarted, **Then** the reconciler replays the drop-copy stream and the OMS state matches the drop-copy state.
2. **Given** a fill exists in the OMS event store but not in the drop-copy, **When** end-of-day reconciliation runs, **Then** an alert is raised on a dedicated mismatch channel with sufficient detail to investigate.
3. **Given** the operator runs the documented recovery runbook, **When** they follow each step, **Then** the system returns to a known-good state and an audit record of the recovery is written to the append-only event store.

---

### User Story 3 - Adding a New Venue Without Touching the Domain (Priority: P2)

A senior engineer has 90 seconds in an interview to answer the question "if I asked you to onboard Cboe Europe tomorrow, what would you do?". They open the repository, point at the shared venue-gateway port interface, point at any existing venue adapter as a template, and explain that adding Cboe means writing one new adapter implementing the same port — the domain code, the OMS, the EMS, the reconciler, the reporting service all stay untouched.

**Why this priority**: The hexagonal-with-venue-as-adapter pattern is the single most important architectural claim of the platform. If this story does not hold, the architecture has failed.

**Independent Test**: Implement a stub adapter for a hypothetical new venue using only the shared port interface, wire it via configuration alone, and observe that the platform routes orders to it without any change to domain or core services.

**Acceptance Scenarios**:

1. **Given** a new venue stub adapter exists, **When** it is registered via configuration, **Then** the OMS can route an order to it using only the existing domain language (Order, Price, Quantity, Side).
2. **Given** the adapter receives an execution from the new venue, **When** it translates to the canonical execution event, **Then** all downstream consumers (reconciliation, reporting, surveillance) process it identically to executions from existing venues.
3. **Given** an engineer reads the venue adapter package, **When** they look at the shared port interface, **Then** the contract for submission, cancellation, replacement, executions, market-data subscription, and health is unambiguous and complete.

---

### User Story 4 - Connecting to Eurex Clearing With Time-Limited Certificates (Priority: P2)

A market-support engineer needs to confirm that the platform's connection to Eurex Clearing (Trade Capture, Position Maintenance, public broadcasts) survives the annual September certificate rotation, that the reconnection logic does not lose messages, and that an alert fires 30 days before any certificate expires.

**Why this priority**: Eurex Clearing is one of two CCPs that matter for Swiss derivatives business; its connection uses messaging semantics and TLS rotation rules that have caused real production incidents at peer banks. Demonstrating mastery here separates the platform from a tutorial.

**Independent Test**: Stop the clearing-broker mock and restart it; observe the platform reconnects automatically without losing any in-flight or queued message. Set a certificate to expire in 29 days; observe the certificate-expiry alert fires.

**Acceptance Scenarios**:

1. **Given** an active connection to the clearing broker, **When** the broker is restarted, **Then** the platform reconnects automatically and continues processing without operator intervention or message loss.
2. **Given** a certificate is rotated, **When** the new certificate is provisioned, **Then** the connection is re-established with the new certificate and the rotation is recorded in the audit log.
3. **Given** a certificate expires in less than 30 days, **When** the daily check runs, **Then** an alert is delivered to the on-call channel with rotation instructions linked.

---

### User Story 5 - Generating a Regulator-Ready Daily Transaction Report (Priority: P2)

A compliance officer runs the daily reporting batch. The system aggregates all reportable trades, generates the FinfraG Art. 39 report (Swiss format) and the MiFID-II RTS-22 report (LSEG TRADEcho format), validates them against the regulator schemas, and either submits to the trade repository / approved reporting mechanism or produces a signed XML file ready for manual upload.

**Why this priority**: Reporting is the most visible deliverable for compliance stakeholders; a Swiss bank cannot ship without it. RTS-22 and FinfraG Art. 39 are the two non-negotiable regulatory artefacts for the in-scope asset classes.

**Independent Test**: Simulate a trading day of 1,000 trades, run the reporting batch, validate the resulting XML against the published regulator schema, and confirm the trade-repository submission stub accepts the report.

**Acceptance Scenarios**:

1. **Given** a day's worth of executed trades exists in the event store, **When** the reporting batch runs, **Then** it produces XML reports that pass schema validation for both FinfraG Art. 39 and RTS-22.
2. **Given** the reports are generated, **When** they are submitted to the trade repository / ARM stub, **Then** the system records the submission acknowledgment with timestamps suitable for audit.
3. **Given** an auditor requests evidence of a specific trade's reporting status, **When** they query by trade identifier, **Then** the system returns the report submission status, the report content, and a tamper-evident hash chain back to the original execution event.

---

### User Story 6 - Real-Time Market Data With Centralised Entitlements (Priority: P2)

A trader subscribes to live FX prices from a major data vendor. Before delivering ticks, the platform checks the trader's entitlements (per-product permissioning codes synchronised from the vendor) and blocks delivery if the user is not entitled. A market-support engineer can revoke an entitlement and see the subscription stop within seconds.

**Why this priority**: Centralised entitlements are a recurring source of incidents at every bank — an unentitled user receiving data is a contractual breach with the vendor and a compliance event.

**Independent Test**: Configure a test trader with entitlements for instrument A but not B. Subscribe to both; observe ticks for A flow and ticks for B are blocked. Revoke A; observe ticks stop.

**Acceptance Scenarios**:

1. **Given** a trader with entitlements for one instrument set, **When** they request a subscription, **Then** entitled instruments stream and unentitled instruments are rejected with a clear reason code.
2. **Given** an entitlement is revoked, **When** the next entitlement-cache refresh occurs, **Then** all in-flight subscriptions for that trader are evaluated and unentitled streams are stopped.
3. **Given** the entitlement source-system is unavailable, **When** a subscription request arrives, **Then** the platform fails closed (denies new subscriptions) rather than failing open.

---

### User Story 7 - Multi-Venue Smart Order Routing for Bonds & FX (Priority: P3)

A buy-side trader submits a corporate-bond RFQ that the platform routes simultaneously to Tradeweb, MarketAxess, and BidFX, collects quotes from up to N dealers, applies an automation rule (dealer count, price tolerance, time-in-comp), executes against the best quote, and writes a transaction-cost-analysis (TCA) record.

**Why this priority**: Multi-venue RFQ is the primary workflow for fixed-income and FX desks at Swiss private banks. It exercises the venue-adapter pattern across three different protocols, demonstrates AiEX-style automation, and produces TCA evidence required for best-execution reports (RTS-28 / its UK/CH analogues).

**Independent Test**: Configure an automation rule, submit an RFQ, observe quotes arrive from all three mock venues, watch the rule choose a winner, and confirm a TCA record is produced.

**Acceptance Scenarios**:

1. **Given** three venue adapters are configured, **When** an RFQ is submitted, **Then** quote requests are sent to all three within 100ms and quotes are aggregated as they arrive.
2. **Given** an automation rule for dealer count and price tolerance, **When** the rule's conditions are met, **Then** the platform automatically executes against the best-quoting dealer and produces a TCA record with relevant benchmarks.
3. **Given** an automation rule's conditions are not met before timeout, **When** the timeout fires, **Then** the platform falls back to manual decision and presents the trader with the collected quotes.

---

### User Story 8 - Surveillance Detects Layering / Spoofing (Priority: P3)

A surveillance analyst reviews market-abuse alerts produced overnight. The system has analysed all order-book events, applied layering / spoofing detection logic, and produced ranked alerts with the underlying order trail, timestamps to microsecond precision, and a suggested investigation pack.

**Why this priority**: Algorithmic-trading governance under MiFID-II RTS-6/7 mandates surveillance. For the Swiss bank, the same is enforced via FINMA market-conduct expectations.

**Independent Test**: Inject a synthetic layering pattern into the historical event stream; observe an alert is produced with the correct order trail and severity ranking.

**Acceptance Scenarios**:

1. **Given** historical order-book events exist, **When** the surveillance batch runs, **Then** layering/spoofing patterns above defined thresholds produce alerts with rank, evidence, and trader identification.
2. **Given** an alert is open, **When** an analyst opens it, **Then** the full event trail (orders, modifications, cancellations, fills) is rendered with consistent UTC timestamps to microsecond precision.
3. **Given** a false positive is identified, **When** the analyst marks it, **Then** the surveillance rules can incorporate the feedback in the next training/tuning cycle.

---

### User Story 9 - Time-Synchronisation Audit Pack for the Regulator (Priority: P3)

A compliance officer is asked by FINMA to evidence the bank's compliance with MiFID-II RTS-25 timestamping requirements. They run the time-sync audit tool which produces a PDF showing, per server, the median and maximum divergence from UTC over the past year, traceable to the upstream grandmaster clock, with a signed hash for tamper evidence.

**Why this priority**: Time-sync evidence is the kind of "boring but mandatory" deliverable that separates serious platforms from tutorials. The hiring manager's question — "how do you evidence RTS-25 compliance?" — has a direct, code-level answer here.

**Independent Test**: Run the audit tool over local PTP/NTP simulator logs from the past 24 hours; observe a PDF artefact is produced with the expected median/max statistics and a verifiable signature.

**Acceptance Scenarios**:

1. **Given** time-sync logs from all trading servers, **When** the audit tool runs, **Then** it produces a per-server divergence summary suitable for regulator submission.
2. **Given** a server's divergence exceeds the regulatory threshold on any day, **When** the audit runs, **Then** that server is flagged and an investigation reference is created.
3. **Given** the audit PDF is produced, **When** any byte is modified, **Then** the embedded signature verification fails.

---

### User Story 10 - Portfolio Walkthrough in 30 Minutes (Priority: P3)

A senior engineering manager at UBS / Julius Bär opens the repository for the first time during an interview. Within 30 minutes they have walked through: the README and tier-1 evidence, the C4 container diagram, one venue adapter end-to-end, the clearing adapter with its certificate-rotation runbook, a property-based FIX codec test running live, the tick-to-trade dashboard, and one architecture decision record showing trade-offs.

**Why this priority**: This is the meta-deliverable of the platform — without a polished walkthrough, the technical content is not surfaced to the audience that matters.

**Independent Test**: Hand the repository to a peer engineer with no prior context; observe whether they can complete the documented 30-minute walkthrough without getting lost.

**Acceptance Scenarios**:

1. **Given** the documented walkthrough script, **When** an engineer follows it, **Then** every linked file, dashboard, runbook, and test exists at the stated location.
2. **Given** the walkthrough's "running tests" step, **When** the engineer triggers the property-based FIX roundtrip test, **Then** it executes against the local environment and passes.
3. **Given** the walkthrough's "open ADR" step, **When** the engineer opens the architecture decision record, **Then** the trade-off rationale, alternatives considered, and decision are clearly documented.

---

### Edge Cases

- **Sequence-number gap on session resume**: When the platform reconnects to a venue and discovers a gap in expected sequence numbers, it must distinguish between application messages (replay required, with possible-duplicate flagged) and admin messages (gap-fill acceptable) and recover without manual intervention.
- **Venue daily reset window**: Some venues reset session sequence numbers at a specific UTC window (e.g., 23:00–23:05). During that window, the platform must hold messages, not error out, and resume cleanly afterwards.
- **Vendor identity collision**: A user's credentials at one vendor must never be reused for another vendor consumer; the platform must enforce that an application identifier is unique per consumer.
- **Bulk file size limit on regulator submission**: When a daily report exceeds the regulator's bulk-file size cap, the platform must split into multiple submissions while maintaining a single logical report identifier.
- **Late drop-copy fill arriving days later**: If a venue resends a fill for a previously-closed trading day, the platform must accept it, allocate it correctly, and treat reporting amendments per regulator rules.
- **Certificate expired at logon**: When the platform attempts to connect with an expired certificate, it must fail closed, alert immediately, and not attempt automatic retry.
- **Unentitled subscription at runtime**: When a previously-entitled user becomes unentitled mid-stream, the data flow must stop within the next entitlement-cache refresh window without delivering further unentitled records.
- **Time-source loss**: If the upstream time source is lost, all trading servers must refuse to accept new orders rather than emit timestamps that may breach regulatory tolerance.
- **Kill-switch triggered**: When an authorised user invokes the kill-switch for a strategy or trader, all in-flight orders for that scope must be cancelled and no new orders accepted until the switch is released, with the action logged immutably.
- **Schema drift in a vendor data dictionary**: When a venue introduces an unknown tag in an inbound message, the platform must log the unknown tag, continue processing the known fields, and surface the drift to the on-call team.

## Requirements *(mandatory)*

### Functional Requirements

#### Order Management & Execution

- **FR-001**: System MUST accept order submissions, modifications, and cancellations through a stable internal API and translate them into the canonical domain language (Order, Price, Quantity, Side, TimeInForce, OrderType).
- **FR-002**: System MUST enforce a single, well-defined order lifecycle state machine, allowing only legal transitions between states (e.g., `Filled` cannot be followed by anything other than a trade-bust).
- **FR-003**: System MUST persist every order state transition to an append-only event store sufficient to reconstruct the full lifecycle of any order on demand.
- **FR-004**: System MUST distinguish between buy-side flow (PMS → OMS → EMS) and sell-side flow (Client Order → SOR → Venue) at runtime via configuration.
- **FR-005**: System MUST support all standard order types from FIX 5.0 SP2 (`OrdType` Tag 40), plus venue-specific extensions where the venue requires them (e.g., Funari, Market-on-Open, Limit-on-Open).

#### Sell-Side Inbound Channel

- **FR-005a**: System MUST act as a FIX acceptor for inbound client orders, supporting both FIX 4.4 and FIX 5.0 SP2 (over FIXT.1.1) on per-client sessions.
- **FR-005b**: Each inbound FIX session MUST be authenticated by `SenderCompID` whitelisting and (where the client requires it) by mTLS client certificate; sessions for unknown `SenderCompID` MUST be refused at logon.
- **FR-005c**: Every inbound order MUST pass through a pre-trade risk gateway in the hot path that enforces per-client limits (notional, fat-finger, max-order-size, restricted-instrument list, kill-switch state) before the order reaches the OMS or any venue adapter; rejected orders MUST be answered with a structured `OrderCancelReject` / `BusinessMessageReject` containing the failed check.
- **FR-005d**: System MUST support DMA (direct market access — pass-through routing), care-order (trader-handled), and algo-wheel (automated venue/strategy selection) routing modes, selectable per inbound order via FIX `HandlInst` (Tag 21) and platform-specific custom tags.
- **FR-005e**: System MUST publish per-client drop-copy back to each client (their orders and fills only) on a separate FIX session distinct from the order-entry session.
- **FR-005f**: System MUST track per-client throttling limits (orders/sec, in-flight orders) and reject excess orders with the FIX-standard rejection reason rather than queueing.

#### Venue Connectivity (Hexagonal Adapters)

- **FR-006**: System MUST expose a single venue-gateway port interface (submit / cancel / replace / executions / market data / health) that every venue adapter implements identically.
- **FR-007**: Venue adapters MUST be the only place in the codebase that contains venue-specific protocol details (FIX tags, binary templates, vendor SDK calls); the domain core MUST contain none of these.
- **FR-008**: System MUST include adapters for the eight in-scope venues / vendors: SIX Swiss Exchange (multiple gateways), Eurex (T7 + C7), Bloomberg, Refinitiv / LSEG, Tradeweb, MarketAxess, BidFX, and CFETS (via Bloomberg / Tradeweb proxy).
- **FR-009**: Adding a new venue MUST be possible by adding a new adapter alone, without modifying the domain, the OMS, the EMS, the reconciler, the reporting service, or any other adapter.
- **FR-010**: Each venue adapter MUST publish health information (connected / disconnected / degraded, last sequence numbers, last heartbeat) suitable for operational dashboards.

#### Drop-Copy & Reconciliation

- **FR-011**: System MUST consume an independent drop-copy stream from each venue that supports one and treat it as the authoritative source-of-truth for filled-quantity reconciliation.
- **FR-012**: System MUST run a continuous reconciler that joins the OMS execution stream and the drop-copy stream on a stable composite key and raises an alert on any mismatch.
- **FR-013**: System MUST support OMS state recovery purely from the drop-copy stream when the OMS event store is unavailable or known to be lagged.

#### Clearing & Settlement

- **FR-014**: System MUST integrate with the Eurex Clearing message bus for trade capture, position maintenance, and public broadcasts, using the messaging semantics specified by Eurex Clearing.
- **FR-015**: System MUST integrate with SIX x-clear / SECOM for Swiss-equity settlement instructions using the relevant ISO 20022 message templates.
- **FR-016**: System MUST automate certificate rotation for all clearing connections and alert at least 30 days before any certificate expiry.
- **FR-017**: System MUST persist clearing-broker reports (e.g., Eurex Common Report Engine) and reconcile them against internal trade records.

#### Market Data

- **FR-018**: System MUST subscribe to and normalise level-1 and level-2 market data from at least one major vendor (Refinitiv) and one terminal-based vendor (Bloomberg).
- **FR-019**: System MUST consume direct exchange feeds (e.g., SIX IMI multicast) where ultra-low-latency processing is required.
- **FR-020**: System MUST publish normalised market data internally on a transport whose latency tier matches the consumer's needs (hot path < 100µs, warm path < 5ms, cold path seconds).

#### Entitlements & Kill-Switch

- **FR-021**: System MUST enforce per-user, per-instrument, per-product entitlements before delivering market data, and refresh entitlements from the source system on a configurable cadence (default ≤ 24 hours).
- **FR-022**: System MUST provide a kill-switch capability that can immediately cancel all open orders for a configurable scope (trader, strategy, desk) and prevent new orders until released, with the action recorded immutably.
- **FR-023**: System MUST treat each vendor application identifier as unique per consumer and never reuse identifiers across consumers.

#### Reporting

- **FR-024**: System MUST generate FinfraG Art. 39 transaction reports in either Swiss or ESMA format and deliver them to the SIX trade repository submission interface.
- **FR-025**: System MUST generate MiFID-II RTS-22 reports in the format expected by the chosen approved reporting mechanism.
- **FR-026**: System MUST generate trade-publication reports (e.g., Trax APA-style for relevant fixed-income asset classes) within the required publication-deferral windows.
- **FR-027**: System MUST validate every generated report against the regulator's published schema before submission and reject internally any report that fails validation.
- **FR-028**: System MUST record the submission acknowledgment from each regulator/repository and retain it for audit.

#### Surveillance

- **FR-029**: System MUST analyse order-book and execution events for at least the layering/spoofing market-abuse pattern and produce ranked alerts with the underlying event trail.
- **FR-030**: System MUST allow surveillance analysts to mark alerts as true/false positives and feed that signal into rule tuning.

#### Time-Synchronisation

- **FR-031**: System MUST synchronise the clocks of all trading-floor servers to within MiFID-II RTS-25 tolerances (≤ 100µs to UTC for trading servers, with logging at 1µs granularity).
- **FR-032**: System MUST log time-sync divergence daily and produce an annual audit pack suitable for FINMA submission, with a tamper-evident signature.
- **FR-033**: Domain code MUST never use wall-clock time directly for regulatory timestamps; it MUST use a clock abstraction that is calibrated against the synchronised hardware clock.

#### Audit & Compliance

- **FR-034**: System MUST write every command (order submission, cancellation, kill-switch, entitlement change) to a tamper-evident audit log with a cryptographic hash chain.
- **FR-035**: System MUST retain audit and event-store data for at least 5 years (MiFID-II RTS-24 baseline) and be configurable up to 10 years where FINMA requires.
- **FR-036**: System MUST archive FIX session logs, FpML/FIXML clearing confirmations, and other regulatory artefacts to write-once-read-many storage.

#### Schemas as Contracts

- **FR-037**: System MUST treat every external message schema (FIX dictionaries, FIXML, FpML, SBE templates, internal Avro schemas) as a versioned artefact stored in source control alongside the code that consumes it.
- **FR-038**: System MUST validate inbound messages against the registered schema and surface unknown fields to operations without halting processing.

#### Operational Capabilities

- **FR-039**: System MUST expose metrics, structured logs, and distributed traces with trace-context propagation across all internal transports, including FIX sessions and binary protocols.
- **FR-040**: System MUST provide a per-venue runbook for at least: session resynchronisation, certificate rotation, vendor identity recovery, entitlement drift, kill-switch drill, and recovery from drop-copy.
- **FR-041**: System MUST be deployable via a single inner-loop command (e.g., `tilt up`) that brings up all services, mocks, and supporting infrastructure for local development.
- **FR-042**: System MUST be reproducibly deployable to a Kubernetes-based UAT environment via declarative composition.

#### Multi-Region Deployment

- **FR-042a**: System MUST deploy active-active across four regions (Zurich ZH4/ZH5, London LD4, New York NY4, Tokyo TY3) with each region capable of accepting inbound client orders, routing to local-region venues, and serving local-region market data.
- **FR-042b**: Aeron Cluster (hot-path consensus) MUST run as one cluster per region; cross-region traffic MUST NOT participate in Raft consensus.
- **FR-042c**: Kafka, Postgres, and the cold-path event store MUST replicate cross-region with documented RPO ≤ 5 seconds and RTO ≤ 60 seconds for any single-region loss.
- **FR-042d**: System MUST support follow-the-sun book-of-business handover at configured cutover times so that the responsible region for a given client/strategy moves with the trading day without dropping in-flight orders.
- **FR-042e**: System MUST route each client order to the most appropriate region (by market hours, client preference, latency to target venue) while ensuring all reporting/audit obligations attach to the correct legal entity regardless of execution region.

#### Security

- **FR-043**: System MUST authenticate users via a centralised identity provider with role-based access control (trader, portfolio manager, compliance, ops, auditor).
- **FR-044**: All internal service-to-service communication MUST use mutual TLS with workload identity.
- **FR-045**: All external secrets (vendor credentials, certificate keys) MUST be stored in a secret-management system with audit logging, never in source control.

#### Testing & Conformance

- **FR-046**: System MUST include venue-conformance tests runnable against in-process or containerised mocks for every supported venue, exercising the standard order lifecycle.
- **FR-047**: System MUST include property-based tests for all message codecs (FIX, FIXML, FpML, SBE) and for the order-state machine, asserting roundtrip and invariant properties.
- **FR-048**: System MUST include resilience tests that inject failures (session drops, partition isolation, time-source skew, broker restarts) and assert automatic recovery.

#### Documentation

- **FR-049**: System MUST publish architecture documentation (C4 context, container, component diagrams), architecture decision records, runbooks, and a glossary, all rendered to a browsable site.
- **FR-050**: System MUST include a 30-minute walkthrough script for portfolio / interview use, with every linked artefact verified to exist at the stated location.

### Key Entities *(domain language only)*

- **Order**: An instruction from a trader to buy or sell a quantity of an instrument at a price under a time-in-force. Has a lifecycle (pending → acknowledged → partially filled → filled / cancelled / rejected).
- **Execution / Fill**: A confirmed transaction against an order at a venue, identified by a venue execution identifier and reconciled across the OMS and the drop-copy stream.
- **Allocation**: A post-trade assignment of a fill to one or more underlying client accounts.
- **Instrument**: A tradeable security identified by canonical identifiers, with attributes including asset class, tick size, currency, and the venues on which it is listed.
- **Legal Entity**: A bank, client, or counterparty with regulatory identifiers (e.g., LEI) used for reporting.
- **Calendar**: A set of trading and settlement holidays per market.
- **Position**: The net holding of an instrument by a portfolio at a point in time.
- **Clearing Trade**: The novated form of an executed trade once accepted by a CCP, with margin obligations attached.
- **Margin Call**: A demand from a CCP for additional collateral.
- **Transaction Report**: A regulatory submission describing one or more executed trades.
- **Abuse Alert**: A surveillance-system finding suggesting a trader or strategy may have engaged in market abuse.
- **Entitlement**: A grant authorising a specific user (and/or application) to receive a specific market-data product or class.
- **Kill Zone**: A configured scope (trader, strategy, desk) that the kill-switch can target.
- **Audit Event**: An immutable record of a system-changing action with a cryptographic link to the previous record.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A trader can submit an order through the local environment and observe the executed fill in the UI within 60 seconds of system start (`tilt up` to first roundtrip).
- **SC-002**: A new engineer can complete the documented 30-minute interview walkthrough without getting lost; every referenced file, dashboard, runbook, and test exists at the stated location.
- **SC-003**: After a simulated 50-minute OMS outage with several hundred fills accumulated in the drop-copy stream, the platform reconstructs OMS state from drop-copy alone with zero lost fills and zero duplicate fills.
- **SC-004**: For every supported venue, a session-disconnect injected by a chaos test results in automatic reconnection and message-gap recovery without operator intervention; recovery completes within 30 seconds.
- **SC-005**: A daily simulated trading load of at least 1,000 trades produces FinfraG Art. 39 and RTS-22 XML reports that pass schema validation and are accepted by the trade-repository / ARM submission stub.
- **SC-006**: For at least one demonstrated venue, a synthetic layering pattern injected into the event stream is detected by the surveillance pipeline and surfaces as an alert with the correct trader identification and microsecond-precision event trail.
- **SC-007**: The annual time-synchronisation audit tool produces a tamper-evident PDF report showing per-server median and maximum divergence from UTC over the audit period, suitable for direct submission to FINMA.
- **SC-008**: Adding a brand-new venue requires changes only inside a new venue-adapter package; no change is required to the domain, OMS, EMS, reconciler, reporting service, or surveillance service. This is verifiable by code-ownership boundaries enforced in continuous integration.
- **SC-009**: The repository's continuous-integration pipeline runs lint, unit, integration, conformance, performance, and security stages on every pull request; the security stage blocks merge on any HIGH or CRITICAL vulnerability in trading-path containers.
- **SC-010**: For ultra-low-latency demonstration purposes, the hot-path order-submission roundtrip on a co-located mock setup achieves a 99th-percentile tick-to-trade latency below 100µs under sustained load, evidenced by a published dashboard.
- **SC-011**: An entitled user receives a market-data subscription within 1 second of request; an unentitled user is rejected with a clear reason within the same window; revoking an entitlement stops the data flow within one entitlement-cache-refresh interval.
- **SC-012**: The platform documents and demonstrates a credible drop-in path to commercial alternatives (FIX engine, time-series database, observability stack) for production hardening, with at least one architecture decision record per drop-in explaining the trade-off.
- **SC-013**: The production-shadow environment sustains 10 million orders/day (mixed submit/cancel/replace) with 99th-percentile end-to-end latency staying within the latency-tier targets (hot < 100µs, warm < 5ms, cold < 50ms), evidenced by a load test in the performance CI stage.
- **SC-014**: The market-data fan-out sustains 50 million ticks/sec across all consumers with no consumer back-pressure overflow, evidenced by a published soak test running for at least 8 hours.
- **SC-015**: The platform supports at least 10,000 concurrent authenticated trader sessions (simulated) without degradation of order-submission p99 latency.
- **SC-016**: The platform sustains at least 200 concurrent inbound FIX client sessions (acceptor side), each with its own throttling limits and pre-trade risk profile, without cross-client interference, evidenced by an isolation test.
- **SC-017**: The pre-trade risk gateway evaluates each inbound order in under 50µs (p99) on the hot path, evidenced by a benchmark in the performance CI stage.
- **SC-018**: A simulated single-region outage in any of the four regions does not lose a single in-flight order; trading continues from the surviving regions with the documented RTO ≤ 60 seconds, evidenced by a regional failover test.
- **SC-019**: At the configured follow-the-sun cutover times, the book of business hands over from one region to the next without dropping any in-flight order or duplicating any fill, evidenced by a daily handover test.

## Assumptions

### Audience & Purpose

- The platform is a **reference implementation and portfolio piece** that targets **production-shadow-grade** depth: a separate `prod-shadow` environment is fully hardened with hardware PTP, real WORM with retention lock, real or vendor-sandbox external integrations, real OSS infrastructure (Postgres, Kafka, Keycloak, OpenBao, Linkerd), and bank-zone network segmentation. Local-dev still uses mocks for the inner-loop. Running on actual customer-facing production traffic at a Swiss bank would still require additional commercial components (e.g., a hardened FIX engine, kdb+/q, ITRS Geneos, Pico Corvil) — these are documented in the architecture decision records as drop-in paths.
- The primary audience is senior engineers, hiring managers, and architecture review boards at Swiss banks (UBS, Julius Bär, ZKB, peers) and adjacent firms. The repository is therefore optimised for readability, walkthrough flow, and code-tree visibility of the in-scope concerns.
- The author profile is a senior individual contributor with 20+ years of IT experience; the 12-week roadmap assumes one experienced engineer working solo.

### Scope Boundaries

- **In scope** for v1: equities (cash and listed derivatives), fixed income, FX, and the regulatory artefacts directly tied to those asset classes (FinfraG Art. 39, MiFID-II RTS-22/24/25, EMIR for OTC derivatives via Eurex Clearing).
- **Out of scope** for v1: full crypto-asset venue connectivity (modelled only as an example of where Rust would be appropriate), full retail private-banking workflows, full structured-products lifecycle, and full custody / corporate-actions handling.
- **Geographic scope**: Four-region active-active deployment in Zurich (FINMA, FinfraG, SIX, Eurex — primary regulatory home), London LD4 (European MTFs/APAs, Refinitiv ELEKTRON, MiFID-II RTS), New York NY4 (US-listed equities, ETFs, FX, US regulatory cross-border), and Tokyo TY3 (Asian-hours coverage, CFETS via Bloomberg / Tradeweb / MarketAxess proxy). Each region carries the full inbound FIX-as-server stack and routes to its local venues and clearing houses.
- **Trading style**: institutional buy-side and sell-side flows; high-frequency proprietary strategies are out of scope (although the hot-path latency tier is demonstrated).

### Architectural Assumptions

- A **polyglot stack is preferred over single-language purity**: Java for the service plane and hot path (mirroring UBS / Morgan Stanley / JPMorgan public stack), Python for adapters / quants / surveillance pipelines (mirroring JPM Athena / BofA Quartz), TypeScript for the trader UI. Rust is used only where it provides demonstrable benefit (e.g., a hypothetical crypto adapter) and never as a stack-wide religion.
- The **hexagonal-with-venue-as-adapter** pattern is non-negotiable. Every venue, clearing house, and data vendor is an outbound adapter behind the same port; domain-core code contains no venue-specific protocol details.
- A **three-tier latency hierarchy** (hot < 100µs / warm < 5ms / cold seconds) is modelled explicitly with distinct transports for each tier; components are placed in exactly one tier.
- **Regulatory compliance is first-class**: time-synchronisation, append-only event sourcing, drop-copy as source-of-truth, and architecture decision records for algorithmic-trading governance are all visible in the directory tree, not annexes.
- Where credible commercial alternatives exist (e.g., the choice between an open-source FIX engine and a sub-microsecond commercial one), the repository ships the open-source path and documents the migration to the commercial drop-in. This is essential for tier-1 credibility.

### Operational Assumptions

- Local development uses container-based mocks for every external dependency (venues, clearing brokers, market-data vendors, time grandmaster). No external network access or vendor account is required to bring the platform up locally.
- Continuous integration runs on a public CI provider (GitHub Actions) with a job matrix covering lint, unit, integration, conformance, performance, and security.
- The deployment substrate is Kubernetes; cloud-managed Kubernetes (Azure AKS) is preferred to mirror the documented public stack of the target employer. Co-location bare-metal deployment for hot-path adapters in the four trading-region POPs (Equinix ZH4/ZH5, LD4, NY4, TY3) is the primary path for ULL components, with the bank's enterprise data centres hosting the rest. Kafka uses MirrorMaker 2 (or equivalent) for cross-region replication; Postgres uses a globally-replicated managed service (e.g., Aurora Global Database) or equivalent.

### Roadmap & Delivery

- Delivery follows the six-phase, twelve-week roadmap described in the input blueprint, with Phase 1 (skeleton + first venue end-to-end) being the immediate next deliverable. Each phase has a documented demo criterion that constitutes its acceptance signal.
- The total effort estimate is approximately 1,000–1,200 engineer-hours, executable as a 12-week full-time solo effort or a 6–9 month parallel-to-day-job effort.
- Subsequent specifications (`/speckit.specify` invocations) will refine each phase into shippable user stories rather than re-specifying the whole platform.
