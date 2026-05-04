# Phase 0 Research: Swiss Trading & Market Support Platform

This document records the technology choices that emerge from the spec + clarifications and the reasoning behind each. The format is fixed: **Decision → Rationale → Alternatives considered → Tier-1 evidence**.

The clarifications fixed three high-impact constraints that drive the rest of the choices:

- **Sell-side prime broker scale** (10M orders/day, 50M ticks/sec, 10k traders, 200 inbound FIX clients).
- **Production-shadow-grade depth** (real OSS infra, real WORM, hardware PTP, vendor-sandbox external integrations).
- **Four-region active-active** (Zurich + LD4 + NY4 + TY3) with follow-the-sun handover.

All decisions below were made with these constraints in view. Where a commercial drop-in is the tier-1 norm but conflicts with the OSS-reference-implementation principle, the OSS choice is taken and the commercial drop-in is documented as a recorded migration path.

---

## R-001: FIX engine — split between QuickFIX/J and Artio

- **Decision**: Use **QuickFIX/J 2.3.2** for standard-throughput vendor sessions (Tradeweb, MarketAxess, Refinitiv, Bloomberg-fallback, Eurex T7 FIX-Gateway fallback) and the bulk of the inbound FIX-as-server tier; use **Artio** (Real Logic) for the high-throughput inbound-FIX-acceptor partition (where 200 concurrent client sessions sustain at sell-side scale would saturate QuickFIX/J's MINA I/O) and the SIX/Eurex ULL hot-path adapters.
- **Rationale**: QuickFIX/J ships a Spring-Boot-Starter, JDBC-backed `JdbcStoreFactory`, Micrometer integration, and is the de-facto standard at Swiss/European banks — the talent pool exists. Its limitation (~10–30k msg/s/session) is fine for vendor sessions and most clients. Artio is built on Aeron, supports iLink-3, achieves sub-100µs, and is the only OSS Java FIX engine that survives sell-side prime broker throughput. Splitting the two means each is used where it shines.
- **Alternatives considered**:
  - *Single engine (QuickFIX/J only)*: would fail SC-010 / SC-016 / SC-017 under sell-side load; MINA I/O bottlenecks at ~30k msg/s/session.
  - *Single engine (Artio only)*: steep learning curve for every vendor session; loses the Spring-Boot ergonomics and Micrometer auto-config.
  - *Chronicle FIX*: commercial, off-heap, sub-µs, but vendor lock-in; documented as the production drop-in for the inbound tier in an ADR.
  - *OnixS / B2BITS*: commercial, exchange-certified; documented as the production drop-in for hot-path adapters.
  - *fefix (Rust)*: pre-1.0, "wildly unstable"; no tier-1 evidence.
- **Tier-1 evidence**: QuickFIX/J broadly used at vendor-products (smartTrade, Macdonald) and Spring-Boot trading shops; Artio shares its codebase with what HSBC and Man Group run in production (Real Logic / Adaptive). Chronicle FIX deployed at Raiffeisen Bank (publicised 2023). OnixS lists 300+ blue-chip firms.

---

## R-002: Service framework — Spring Boot 3 (with FastAPI for Python services)

- **Decision**: **Spring Boot 3.3 on Java 21** as the service-plane backbone for OMS, EMS, venue adapters, clearing adapter, reporting, entitlements, audit, reconciler. **FastAPI 0.110 on Python 3.12** for `reference-data-service`, `surveillance-service`, and `venue-adapter-cfets`.
- **Rationale**: Spring Boot is the documented stack at UBS, Julius Bär, JPMorgan, Morgan Stanley — congruence with the target employer is the dominant criterion for a portfolio piece. Spring Cloud Gateway, Actuator, Micrometer, Spring Security Resource Server, Spring Batch, Spring Statemachine are all directly applicable. FastAPI is the matching Python framework for the quants/surveillance pipelines (mirroring JPM Athena and BofA Quartz).
- **Alternatives considered**:
  - *Quarkus*: GraalVM native compile, lower cold-start, reactive — but no tier-1 trading production publicised. Red Hat tutorial for QuickFIX/J + Quarkus exists but no bank deployment.
  - *Micronaut*: AOT-compile, small footprint — no tier-1 trading evidence.
  - *Plain Vert.x / Helidon SE*: too low-level; loses ecosystem benefits.
- **Tier-1 evidence**: UBS, JPMorgan, Morgan Stanley, Julius Bär all hire for "Spring Boot + Java 21" on trading platforms (publicly visible job postings). JPMorgan Athena and BofA Quartz are Python-on-FastAPI-style internal platforms.

---

## R-003: Hot-path messaging — Aeron OSS + SBE + LMAX Disruptor

- **Decision**: **Aeron 1.45 + SBE 1.31 + LMAX Disruptor 4** as the hot-path trinity. Aeron Cluster (Raft) for matching/EMS replication. Per-region cluster (5 nodes, quorum 3); cross-region replication via Kafka MirrorMaker 2 of the cold-path event store rather than via Aeron.
- **Rationale**: Aeron + SBE is the single dominant tier-1-bank choice for sub-100µs deterministic latency. SBE is zero-copy/zero-alloc; Disruptor is the proven single-writer queue for matching-thread isolation; Aeron Cluster provides Raft consensus for replicated state without a heavy operator burden. The combination is the same set of building blocks Adaptive uses to ship "Hydra" and what HSBC, Man Group, Brevan Howard, SIX SIC5, and EDX run in production (per public Aeron MeetUp talks).
- **Alternatives considered**:
  - *Aeron Premium*: ~30–40µs network latency vs ~50–100µs OSS — improvement, but commercial. Documented as a drop-in for the Equinix POP servers.
  - *Solace PubSub+ appliances*: 100–500µs, deployed by RBC at scale (50 appliances, 118B msg/day) but commercial and at higher latency than Aeron — kept as a documented drop-in for cross-region event mesh.
  - *Chronicle Queue alone*: persistent < 1µs intra-process, but not network — insufficient as the only hot-path transport. Used in this stack only as the in-process compliance journal.
  - *Solace + IBM MQ Low Latency*: reasonable for warm path; documented as drop-in.
  - *Kafka for hot path*: 5–15ms p99, by orders of magnitude too slow.
- **Tier-1 evidence**: HSBC Equities (Grahame Rogers, Aeron MeetUp 2024), Man Group FX ($1.5T/year, Wantola/Raggatt), Brevan Howard, SIX SIC5 (Stefan Ferstl, 35M payments/day), EDX/EDXM (73µs RTT), Coinbase, Bullish, DriveWealth, LSEG/KCx.

---

## R-004: Event spine — Apache Kafka 3.7 (KRaft) + Apache Flink 1.18

- **Decision**: **Apache Kafka 3.7 in KRaft mode** via Strimzi operator on K8s; **Apache Flink 1.18** for surveillance and post-trade exactly-once stream processing; **Kafka Streams** for light transforms; **MirrorMaker 2** for cross-region replication. Confluent Platform documented as commercial drop-in. Redpanda documented as Kafka-API-compatible drop-in.
- **Rationale**: Kafka is the universal sell-side / post-trade / surveillance standard at ING, Capital One, RBC, Robinhood, Euronext (Optiq). KRaft removes the ZooKeeper operational tax. Strimzi gives a clean K8s deployment story across all four regions. Flink is the only stream processor with true streaming semantics + exactly-once for surveillance pipelines that span multi-day windows.
- **Alternatives considered**:
  - *Redpanda*: Kafka-API, single binary, lower latency (sub-5ms p99) — kept as a drop-in. Concern: BSL→Apache 2.0 happens 4 years after each release, so true OSS is delayed.
  - *Apache Pulsar*: multi-tenancy and tiered storage are nice but operational burden is higher and bank evidence is thinner.
  - *Spark Streaming*: micro-batch, not true streaming; loses exactly-once guarantees on multi-day windows.
- **Tier-1 evidence**: ING, Capital One, RBC, Robinhood, Euronext (Optiq), anonymous tier-1 bank running 1.6M msg/s on Confluent. Flink at ING (fraud), Garanti BBVA.

---

## R-005: Persistence — multi-tier (Postgres + QuestDB + ClickHouse + Redis + S3 WORM)

- **Decision**: **PostgreSQL 16 (Aurora Global Database in cloud / self-managed in DC)** for transactional state, allocations, reference data. **QuestDB 9.x** for intraday tick hot tier (7-day retention). **ClickHouse 24.x** for multi-year tick research. **Redis 7** for hot-state cache, FIX-session cache, entitlement cache. **S3 / MinIO with Object Lock** for WORM archival (FIX logs, FpML/FIXML confirmations, audit log, regulator submission acks). **Postgres + Outbox + Aeron Archive** as the per-region append-only event store with cryptographic hash chain (SHA-256 prev-hash linked) and S3 cross-region mirror. **kdb+/q documented as the tier-1 production drop-in for both tick tiers.**
- **Rationale**: Different access patterns require different stores; conflating them costs throughput at sell-side scale. Aurora Global DB gives cross-region Postgres replication with documented blue/green at LSEG. QuestDB benchmarks at OHLCV 25ms vs kdb+ 109ms (publicised) — sufficient for the OSS hot tier. ClickHouse is the columnar warm tier of choice (Deutsche Bank publicised). Redis is universal. S3 with Object Lock provides RTS-24 / FINMA WORM. Hash-chained event store with Outbox + Aeron Archive replays deterministically. kdb+/q remains the tier-1 production drop-in (every tier-1 sell-side bank uses it) but is commercial.
- **Alternatives considered**:
  - *Single OLTP database for everything*: would not scale to SC-014 50M ticks/sec.
  - *kdb+/q as the single time-series store*: tier-1 standard, commercial; documented as drop-in.
  - *InfluxDB / TimescaleDB*: lower throughput than QuestDB at the same hardware footprint.
  - *Self-managed Postgres Patroni cluster instead of Aurora Global DB*: more ops burden and slower cross-region failover than Aurora.
- **Tier-1 evidence**: LSEG (Aurora PG Global DB Blue/Green, AWS blog 2024); QuestDB benchmarks public; ClickHouse at Deutsche Bank (publicised); kdb+/q at Morgan Stanley, Goldman, RBC, UBS, Deutsche, Citi, Barclays, BAML, JPMorgan, Nomura, BNY (KX customer references).

---

## R-006: Observability — Prometheus + Grafana + OpenSearch + OpenTelemetry (with commercial drop-ins)

- **Decision**: **Prometheus + AlertManager + Grafana + Mimir** for metrics; **Vector → Loki** (30-day ops) **+ OpenSearch 2** (7-year FIX archive WORM) for logs; **OpenTelemetry SDK + OTel Collector + Tempo** for traces. **ITRS Geneos** documented as the trading-floor commercial drop-in (9-of-10 tier-1 banks claim by ITRS); **Pico Corvil** documented as the wire-tap-based tick-to-trade observation drop-in.
- **Rationale**: OSS observability has matured to the point of being credible by itself for everything except wire-data tick-to-trade observation (which still requires Corvil-class hardware) and trading-floor "single pane of glass" dashboards (which Geneos owns at tier-1). Both gaps are honestly documented as drop-ins. OpenSearch is Apache 2.0 (vs Elastic's licensing pivot) and the drop-in semantics are 1:1.
- **Alternatives considered**:
  - *Elastic Stack*: licensing pivot post-2021 disqualifies it for an OSS reference; replaced by OpenSearch.
  - *Datadog* across the board: tier-1 alternative, commercial, documented in ADR.
  - *Splunk* for logs: commercial; documented in ADR.
- **Tier-1 evidence**: Geneos at 9/10 tier-1 banks (ITRS); Pico Corvil at Citi, Saxo, Flow Traders, Tradition.

---

## R-007: Secrets / PKI / mesh / identity — OpenBao + cert-manager + Linkerd + SPIFFE + Keycloak

- **Decision**: **OpenBao** (the OSS Vault fork — Vault's BSL pivot in 2023 motivated the fork) for KV secrets, PKI, Transit. **cert-manager 1.15** for certificate lifecycle. **Linkerd 2.16** service mesh with **SPIFFE/SPIRE** workload identity for in-mesh mTLS. **Keycloak 25** as the OIDC/OAuth2/SAML IdP.
- **Rationale**: Linkerd's Rust proxy has < 1ms overhead vs Envoy/Istio's higher footprint and is operationally simpler — at sell-side latency targets the proxy overhead matters. SPIFFE/SPIRE attestation matches the workload-identity-as-cert pattern (`spiffe://swiss-tms.local/ns/<env>/sa/<service>`). OpenBao matches Vault's API surface so the migration story is documented and clean. Keycloak is the standard self-hosted IdP for European banks.
- **Alternatives considered**:
  - *Istio*: more features, higher overhead; rejected for hot-path-adjacent services.
  - *HashiCorp Vault*: BSL licence post-2023; OpenBao is the OSS continuation.
  - *Auth0/Okta*: commercial; valid drop-in.
- **Tier-1 evidence**: Linkerd benchmarks (publicised). Keycloak ubiquitous in European banking.

---

## R-008: Multi-region replication — Aurora Global DB + Kafka MirrorMaker 2 + S3 cross-region

- **Decision**: **Aurora Global Database** for Postgres cross-region (read replicas in non-primary regions; promotable in < 1 minute). **Kafka MirrorMaker 2** for topic replication with active-active per-region writes and offset-translation for consumers. **S3 cross-region replication** for the WORM archival mirror. **Per-region Aeron Cluster** (Raft can't cross WAN at the latency budget). **Region-router service** (`apps/region-router/`) implementing follow-the-sun book-of-business handover at configured cutover times.
- **Rationale**: At 4-region active-active sell-side scale, the only credible OSS path is per-region clusters with cold-path event-spine replication. Aurora Global DB is the documented LSEG choice (AWS blog 2024). MirrorMaker 2 with offset-translation is the cleanest active-active Kafka topology. The region-router is needed for follow-the-sun; the spec already drives a custom service for this.
- **Alternatives considered**:
  - *Self-managed cross-region Postgres (Patroni)*: more ops burden, slower failover.
  - *Kafka stretched cluster across regions*: latency and split-brain risk make it unacceptable.
  - *Single global Aeron Cluster*: incompatible with hot-path latency budget.
- **Tier-1 evidence**: LSEG Aurora PG Global DB blue/green (AWS blog 2024); Confluent / RBC / Capital One MirrorMaker 2 deployments (public talks).

---

## R-009: Time synchronisation (RTS-25) — Meinberg LANTIME M3000 + ptp4l/phc2sys + Solarflare/Mellanox HW timestamping

- **Decision**: Per-region **Meinberg LANTIME M3000** PTP grandmaster, boundary-clock per cabinet, **`ptp4l` + `phc2sys`** on Solarflare or Mellanox NICs with hardware timestamping. **`chrony`** as NTP fallback for non-trading servers. Local-dev / CI uses `mocks/ptp-grandmaster-sim/` (software PTP master). Annual audit pack generated by **`tools/ptp-audit-report/`** (Go tool) producing signed PDF traceable to GM logs.
- **Rationale**: At sell-side scale + RTS-25 + FINMA evidence requirement, hardware grandmasters are non-negotiable. Solarflare and Mellanox are the only NIC vendors with credible HW timestamping at the relevant message rates. `ptp4l` + `phc2sys` is the OSS reference implementation that all the bank tier-1 deployments use under the covers. The audit-pack generator is a small Go tool that consumes `ptp4l` + `phc2sys` daily logs from OpenSearch and emits a signed PDF.
- **Alternatives considered**:
  - *Software PTP only*: cannot stay within ≤100µs to UTC under sell-side load with HW-timestamping NICs absent.
  - *NTP only*: ~ms-class accuracy, fails RTS-25.
  - *GPS receivers per host*: more expensive and fragile than a centralised grandmaster.
- **Tier-1 evidence**: Meinberg LANTIME deployed at SIX, every major exchange and most tier-1 banks (vendor references public).

---

## R-010: Inbound FIX-as-server architecture — Artio acceptor + per-client session config + Aeron-IPC pre-trade-risk handoff

- **Decision**: `apps/inbound-fix-acceptor/` runs on **Artio** (the only OSS Java engine that sustains 200 concurrent sessions at sell-side throughput). Each client session is configured per `SenderCompID` with mTLS client cert, per-session throttle (orders/sec, in-flight count), per-session whitelist (instruments, asset classes, order types). On every inbound `NewOrderSingle / OrderCancelRequest / OrderCancelReplaceRequest`, the acceptor publishes the order on a **dedicated Aeron IPC channel** to `apps/pretrade-risk-gateway/` running on the same physical host (zero-copy IPC). The risk gateway (Disruptor single-writer) evaluates per-client and per-firm risk rules in < 50µs p99 (SC-017) and either republishes the cleared order to the SOR / venue adapters or replies with a structured FIX `Reject (35=3)` / `BusinessMessageReject (35=j)`.
- **Rationale**: Co-location of the acceptor and the risk gateway on the same physical host enables Aeron IPC (~0.25µs RTT) rather than Aeron UDP, which is essential to meet the < 50µs p99 risk-eval target. Per-client whitelisting and throttling at the session boundary stops malicious / runaway clients before they touch the OMS or the venue. Drop-copy back to the client is on a separate FIX session (FR-005e) to avoid head-of-line blocking from order-entry traffic.
- **Alternatives considered**:
  - *Single FIX gateway combining session and risk*: violates single-responsibility and prevents independent scale-out of the risk evaluator.
  - *Risk in the OMS*: puts a hot-path component in a warm-path service, violates latency-hierarchy invariant.
  - *External risk service over network*: blows the 50µs p99 budget instantly.
- **Tier-1 evidence**: Goldman / JPM / Morgan Stanley DMA inbound architectures all separate the FIX session from the risk gateway by the same logic; co-location of acceptor + risk on the same physical host is the documented norm at FIA-EPTA member firms.

---

## R-011: Pre-trade risk gateway — Disruptor single-writer + cached limits in shared Aeron map

- **Decision**: `apps/pretrade-risk-gateway/` is a single-process service with a single Disruptor ring buffer; one consumer thread evaluates rules. Per-client and per-firm limits are loaded at startup from `apps/entitlements-service/` and refreshed via Kafka topic `warm.entitlements.limit-update.v1`. A read-only `Long2LongHashMap` (Agrona) holds counters; updates use `LongAdder` style striped counters. Rule evaluator implemented as a small DSL (limit type → check function) so new rules don't require re-architecture.
- **Rationale**: Single-writer Disruptor is the canonical sub-µs evaluator pattern (LMAX Exchange origin story). Off-heap Agrona maps avoid GC pauses. DSL avoids hard-coding rules into the hot-path code; new asset classes' rules can be added by config alone.
- **Alternatives considered**:
  - *Drools / a generic rule engine*: way too slow for hot-path.
  - *Distributed risk over network*: see R-010.
  - *Hard-coded per-rule Java methods*: works for v1 but ossifies the hot-path on every rule change.
- **Tier-1 evidence**: LMAX Exchange's pre-trade-risk pattern; FIA-EPTA Market-Access-Framework recommendations.

---

## R-012: Schemas as versioned contracts — Apicurio Registry + Pact + JAXB / SBE codegen

- **Decision**: All Kafka topic schemas in **Apache Avro**, registered to **Apicurio Registry**. Service↔service REST/gRPC contracts in **gRPC `.proto`** + **OpenAPI 3** with **Pact** consumer-driven contract tests. FIX dictionaries in **QuickFIX XML** (FIX 4.4, 5.0 SP2, FIXT 1.1, plus per-venue dialects under `contracts/fix/venues/`). FIXML in vendored **XSDs**, code generated to Java via JAXB (Gradle task in `tools/codegen/`). FpML 5.12 same. SBE schemas in **SBE XML**, code generated to Java via the SBE `SbeTool`. Every PR that changes a schema MUST also update or add a contract test.
- **Rationale**: Schemas-as-code is the single most reliable defence against the "tag 7777 is also taken now" regression that periodically takes down trading platforms. Apicurio is the OSS Schema-Registry-compatible registry. Pact is the standard for consumer-driven contract testing.
- **Alternatives considered**:
  - *Confluent Schema Registry*: tier-1 commercial drop-in (Confluent licence semantics changed; Apicurio is the cleanest OSS).
  - *Protobuf for everything*: would lose the FIX-dictionary native tooling; FIX is canonically defined in XML.
- **Tier-1 evidence**: Apicurio at Red Hat customer banks; Pact at Deutsche Bank, Atlassian, Capital One.

---

## R-013: Build & dev — Gradle 8 multi-project + uv + go.work + Cargo + Tilt

- **Decision**: **Gradle 8.10** as the primary build driver (~80% of the code is JVM). **uv 0.4** (Astral) for Python workspace. **`go.work`** for Go workspaces. **Cargo workspace** for optional Rust. Top-level **Makefile** invokes each driver task-by-task (`make build`, `make test`, `make scaffold`, `make smoke`). **Tilt 0.33** + **kind / k3d** for inner-loop multi-service development. **Helm 3 + Helmfile + Kustomize** for environment composition. **Terraform** for cloud infra; **Ansible** for bare-metal trading floor.
- **Rationale**: Gradle multi-project is the path of least resistance for a JVM-heavy mono-repo and avoids Bazel's complexity tax. uv is the fastest emerging Python toolchain and supports workspace mode natively. `go.work` is the official Go workspace mechanism. Helmfile is the deployable composition layer that K8s + Helm alone don't provide. Tilt is the documented inner-loop tool used by Adaptive (and visible in the input blueprint).
- **Alternatives considered**:
  - *Bazel*: more powerful for very large multi-language mono-repos but the operational tax is high; Gradle + uv + go.work is sufficient.
  - *Maven*: lacks Gradle's flexibility for codegen + per-module SBE/JAXB tasks.
  - *Skaffold*: Tilt has better multi-service UI ergonomics.
- **Tier-1 evidence**: Adaptive use Tilt for Hydra inner-loop (publicised). Astral uv adoption broad and growing.

---

## R-014: CI/CD — GitHub Actions matrix + Trivy/Grype + Syft (SBOM) + cosign (Sigstore) + Helmfile deploy

- **Decision**: **GitHub Actions** matrix: `lint → unit → integration → conformance → performance → security → deploy-dev`. Performance is non-blocking but publishes regression tracking to Mimir-backed Grafana. Security blocks merge on HIGH/CRITICAL CVEs in trading-path containers. Image scan with **Trivy** + **Grype**, SBOM with **Syft**, sign with **cosign / Sigstore**. Merge to `main` triggers `helmfile -e dev sync`.
- **Rationale**: GitHub Actions has the broadest marketplace of integrations (OIDC to Azure / AWS, sigstore, etc.). The matrix mirrors the regulatory expectation that every change is lint-checked, unit-tested, integration-tested against real OSS infra, conformance-tested per venue, performance-tracked, and security-scanned before deployment. SLSA Level 3 is the documented target.
- **Alternatives considered**:
  - *GitLab CI*: similar capabilities; documented as drop-in if the bank uses GitLab Enterprise.
  - *Jenkins*: tier-1 default but operationally heavier; only used as drop-in.
- **Tier-1 evidence**: SLSA Level 3 referenced across modern bank-engineering blogs (Capital One, ING).

---

## R-015: Conformance, property-based, and chaos testing — FIXimulator + Esprow drop-in + jqwik / Hypothesis + Chaos Mesh + Toxiproxy

- **Decision**: **FIXimulator** containerised in `mocks/fiximulator/` driving conformance for every initiator session, fed with the per-venue dictionaries. Custom mocks for binary protocols (`mocks/six-mts-stub/`, `mocks/eurex-amqp-broker/` using **Apache Qpid Broker-J**, `mocks/bloomberg-stub/`, `mocks/refinitiv-ema-provider/` using EMA `OmmProvider`, `mocks/cfets-via-tradeweb-mock/`). **Esprow ETP C-Box** documented as the commercial customer-onboarding certification drop-in. **jqwik 1.8** for Java property tests, **Hypothesis** for Python — every codec, state machine, and FIX dictionary roundtrip has property-based coverage. **Chaos Mesh** for cluster-level chaos (FIX session drop, Kafka partition isolate, PTP skew injection, AMQP broker restart, Aeron Cluster leader kill). **Toxiproxy** for finer-grained network conditions in unit / integration tests.
- **Rationale**: FIXimulator is the OSS standard for sell-side conformance. Esprow is the commercial drop-in for actual customer onboarding (paid certification). Property-based tests catch the "tag 9 / tag 10 are off-by-one" regressions deterministically. Chaos Mesh is the K8s-native chaos tool; Toxiproxy is the standard finer-grained one.
- **Alternatives considered**:
  - *Greenline*: commercial, comparable to Esprow; documented.
  - *QuickCheck for Scala / ScalaCheck*: not relevant (no Scala in scope).
- **Tier-1 evidence**: FIXimulator widespread; Esprow ETP at multiple sell-side onboarding programs; Chaos Mesh CNCF graduated.

---

## R-016: Reporting (FinfraG Art.39, RTS-22, Trax APA, EMIR) — Spring Batch + LSEG TRADEcho ARM + DTCC GTR / REGIS-TR

- **Decision**: `apps/reporting-service/` is **Spring Batch**-based (each report type is a `Job`). Daily batch generates FinfraG Art.39 XML (Swiss or ESMA format), submits via SFTP to **SIX Trade Repository**. RTS-22 generated and submitted to **LSEG TRADEcho** ARM (REST API). Trax APA via **MarketAxess Trax** (FIXT.1.1 + FIX 5.0 SP2 + EP228 + Trax custom tags) using **TradeCaptureReport(AE) → TradeCaptureReportAck(AR)** with daily 23:00–23:05 GMT session reset; CSV-SFTP fallback for > 3GB. EMIR via **DTCC GTR** + **REGIS-TR**. Every report is schema-validated before submission and rejected internally on validation failure (FR-027). Submission acknowledgments persisted to Postgres `regulatory_submission` table (FR-028).
- **Rationale**: Spring Batch is the standard for daily scheduled regulatory submissions in JVM shops (UBS, JPMorgan). Schema-validation pre-submit closes the most common reporting incident class.
- **Alternatives considered**:
  - *Apache Airflow as orchestrator*: better for arbitrary DAGs, but Spring Batch's restart / idempotency / chunk-step semantics fit regulatory batch better.
  - *Direct ARM integrations without Spring Batch*: no checkpoint/restart story; rejected.
- **Tier-1 evidence**: Spring Batch in JPMorgan and UBS (job postings reference it); LSEG TRADEcho is the dominant ARM in Europe; SIX Trade Repository is the FinfraG TR.

---

## R-017: Surveillance — Apache Flink layering/spoofing detection + analyst feedback loop

- **Decision**: `apps/surveillance-service/` is a **Flink 1.18** application reading from Kafka topic `cold.exec.fill.v1` and `cold.book.event.v1`, applying layering and spoofing patterns over a sliding window (default 5-minute window, 1-second slide). Alerts written to Kafka `cold.surveillance.alert.v1` and indexed in OpenSearch for analyst review. Analyst marks via REST API; feedback feeds a Kafka topic `cold.surveillance.feedback.v1` consumed nightly by a tuning job.
- **Rationale**: Flink's exactly-once semantics are necessary for surveillance because miscounting a single event can change a layering finding. The analyst feedback loop is the difference between a surveillance system that produces useful alerts and one that produces noise.
- **Alternatives considered**:
  - *Kafka Streams*: weaker windowing, no exactly-once across multi-day windows.
  - *Spark Structured Streaming*: micro-batch, latency-of-detection budget worse.
- **Tier-1 evidence**: Flink at ING (fraud), Garanti BBVA, Lyft (publicised; non-bank but same patterns).

---

## R-018: Localization — defer; UI is English-first with i18n hooks in place

- **Decision**: Trader UI is **English-first** for v1 with **react-i18next** wired in and a translation-key audit running in CI. German/French/Italian translations are **out of scope for v1** but the architecture does not block them. (Clarification Q deferred at session end; this is the documented default.)
- **Rationale**: The Swiss-bank context (Basel) implies multi-language eventually, but the input blueprint is German-language and the domain language (Order, Side, Quantity) is English-first by industry convention. Adding i18n keys without delivering translations is the cheapest defensive default and keeps the v1 UI scope contained.
- **Alternatives considered**:
  - *Multilingual from v1*: 4× translation cost without a clear stakeholder driver.
  - *No i18n at all*: locks out future translation without a refactor.
- **Tier-1 evidence**: Default pattern at Julius Bär, Pictet, ZKB internal trading apps (English-first, with limited DE/FR for client-facing only).

---

## R-019: Drop-copy venue support matrix

- **Decision**: First-class drop-copy support (real, reconciled) for **SIX, Eurex, Tradeweb, MarketAxess, BidFX, Bloomberg EMSX**. **Refinitiv** does not provide a drop-copy concept (it is a market-data vendor primarily; orders go via EMSX or direct venue) so its adapter does not produce drop-copy. **CFETS** drop-copy is sourced from the Tradeweb / Bloomberg proxy adapter (the same channel the orders use). Each venue adapter publishes drop-copy on a dedicated Kafka topic `warm.dropcopy.<venue>.v1` consumed by `apps/reconciler-service/`.
- **Rationale**: Drop-copy is venue-specific; not every venue supports it. This decision establishes a clear matrix so the reconciler knows which streams are authoritative and the engineer-on-call has an unambiguous answer to "is venue X drop-copy live?".
- **Alternatives considered**:
  - *Treat OMS as authoritative for venues without drop-copy*: weaker audit story; documented as the fallback.
- **Tier-1 evidence**: SIX / Eurex / Bloomberg / Tradeweb / MarketAxess / BidFX all publish drop-copy as a documented session product.

---

## R-020: Sell-side flow — DMA / care-order / algo-wheel selectable per inbound order

- **Decision**: Inbound FIX `HandlInst` (Tag 21) drives the routing mode: `1=Automated execution, no broker intervention` → DMA pass-through, `2=Automated, broker intervention OK` → algo-wheel, `3=Manual order` → care order (queues to a trader's UI). Custom Trax-style platform tags allow finer routing-mode selection. Algo-wheel implementation lives in `apps/ems-service/` `algo/` package with strategies VWAP, TWAP, POV, IS, plus a Plug-In SPI for new strategies.
- **Rationale**: `HandlInst` is the FIX-canonical knob. DMA and algo-wheel are the two big-ticket sell-side flows; care order is the safety net for high-touch institutional clients. SPI for new strategies prevents EMS code from re-architecting on every new algo.
- **Alternatives considered**:
  - *Single mode (DMA only)*: misses the algo-wheel revenue (and is a sell-side product gap).
  - *Routing-mode in custom tag instead of HandlInst*: non-standard; rejected.
- **Tier-1 evidence**: Goldman, Citadel, Morgan Stanley algo-wheel architectures (publicised at QuantMinds / ai+t industry talks).

---

## R-021: Region-router (follow-the-sun) — small Java service driven by a config DSL

- **Decision**: `apps/region-router/` is a small Spring Boot service that reads a YAML config of routing rules (per-client preferred region, per-instrument primary venue region, per-asset-class market-hours window) and tags each inbound order with a target region before it reaches the inbound-FIX-acceptor's risk gateway. Cutover times (e.g., Tokyo → London at 06:00 UTC, London → New York at 14:00 UTC, New York → Tokyo at 22:00 UTC) drive the daily handover. In-flight orders at cutover are replicated cross-region via Kafka MirrorMaker 2; the new region picks up using the OMS event store.
- **Rationale**: A single dedicated service for region routing keeps the logic explicit, testable, and easy to evolve. Cutover times in config (not code) reduce ops risk.
- **Alternatives considered**:
  - *Routing in the inbound-FIX-acceptor*: violates single-responsibility; the acceptor should be transport-only.
  - *Routing in the OMS*: too late in the pipeline; routing must precede the risk gateway to keep regional load balanced.
- **Tier-1 evidence**: Goldman / JPM follow-the-sun trading systems are documented as having a dedicated routing layer (non-public details, but the pattern is industry-standard).

---

## R-022: Hardware-bound bare-metal vs cloud — co-lo bare metal for ULL + AKS for everything else

- **Decision**: Hot-path adapters (SIX OTI, Eurex T7 ETI, BidFX Pixie firm-quotes, MarketAxess hot quotes) deploy on **bare-metal Solarflare or Mellanox-NIC servers in the local Equinix POP** (ZH4/ZH5, LD4, NY4, TY3) running RHEL 9 with `tuned` profiles for low latency. Aeron UDP between hot-path servers uses LCN. Everything else (OMS, EMS, reporting, surveillance, entitlements, audit, reconciler, region-router, K8s control plane, Kafka, Postgres, OpenSearch, Grafana, Keycloak, OpenBao) runs on **AKS** clusters in the matching Azure region (`switzerlandnorth`, `uksouth`, `eastus`, `japaneast`).
- **Rationale**: Cloud K8s cannot guarantee the sub-100µs hot-path target; bare metal in the venue's own POP is the only credible deployment topology. Everything that doesn't need that latency runs on AKS to inherit the operational ergonomics. Crossing the AKS↔bare-metal boundary uses Aeron UDP with documented latency-budget rules.
- **Alternatives considered**:
  - *All cloud*: fails SC-010 / SC-017.
  - *All bare metal*: loses operational ergonomics and AKS-based UAT/prod-shadow story.
- **Tier-1 evidence**: Documented venue co-lo deployments at every tier-1 sell-side bank.

---

## Open / deferred items

| Item | Status | Resolution path |
|---|---|---|
| Localization (DE/FR/IT translations) | Deferred (R-018) | i18n hooks in v1; translations in v2+ when stakeholder driver appears |
| Real Bloomberg / Refinitiv accounts for vendor-sandbox prod-shadow | Deferred to ops-readiness phase | Documented in `ops/runbooks/vendor-onboarding.md` (to be created); local-dev uses mocks |
| Real Eurex / SIX certification | Deferred to ops-readiness phase | Esprow / commercial certification is the vehicle |
| ITRS Geneos and Pico Corvil drop-ins | Documented in ADRs only; not deployed in OSS reference | Production hardening track |
| kdb+/q drop-in for tick stores | Documented in ADR; OSS path uses QuestDB + ClickHouse | Production hardening track |
| Constitution ratification | Outstanding (no project principles defined) | Recommend `/speckit.constitution` after this plan |
