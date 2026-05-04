# Implementation Plan: Swiss Trading & Market Support Platform (Reference Mono-Repo)

**Branch**: `001-swiss-tms-platform` | **Date**: 2026-05-03 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-swiss-tms-platform/spec.md`

## Summary

A polyglot, hexagonal mono-repo that delivers a sell-side prime-broker-scale trading and market-support platform deployed active-active across four regions (Zurich, London, New York, Tokyo) and integrating with eight venues / vendors (SIX, Eurex, Bloomberg, Refinitiv, Tradeweb, MarketAxess, BidFX, CFETS) plus their clearing houses. The platform exposes inbound FIX-as-server sessions to external clients with a hot-path pre-trade risk gateway, routes orders through DMA / care-order / algo-wheel modes, persists every state change to an append-only event store with cryptographic hash chain, generates regulator-ready reports (FinfraG Art. 39, MiFID-II RTS-22, Trax APA), and surfaces a 30-minute interview walkthrough for portfolio use. Production-shadow-grade non-functional depth (hardware PTP, real WORM, real OSS infrastructure, vendor-sandbox external integrations) — local-dev still uses mocks for the inner loop.

The technical approach centres on three pillars from the public stacks of UBS, RBC Capital Markets, HSBC Equities, Man Group, and SIX Interbank Clearing: **Java 21 + Spring Boot 3** as the service-plane backbone, **Aeron + SBE + Disruptor + Artio** for the hot path, and **Kafka + Flink** as the warm/cold event spine. Hexagonal-with-venue-as-adapter is the architectural backbone — every external integration sits behind a single port interface in `libs/domain-model/`. This plan covers all 61 functional requirements across all six phases of the 12-week roadmap; per-phase task lists will be refined by `/speckit.tasks`.

## Technical Context

**Language/Version**:
- **Java 21 (LTS)** — primary service-plane and hot-path language (OMS, EMS, venue adapters, clearing adapter, reporting, entitlements, market-data normalisation, pre-trade risk gateway).
- **Python 3.12** — reference-data service, CFETS proxy adapter, surveillance pipelines, scripting/test fixtures.
- **TypeScript 5.4** + **React 18** — trader UI.
- **Go 1.22** — small operational tools (PTP audit reporter, conformance harness drivers).
- **Rust 1.78** — optional, only for a hypothetical crypto-venue adapter and a high-performance codec; not used in v1 deliverables but the workspace is provisioned.

**Primary Dependencies**:
- **Service framework**: Spring Boot 3.3 (Spring Cloud Gateway, Spring Security Resource Server, Spring Batch, Spring Statemachine for OrdStatus, Spring Data JPA via Hibernate 6).
- **FIX engines**: QuickFIX/J 2.3.2 for standard sessions and inbound acceptor (with `io.allune:quickfixj-spring-boot-starter:2.10`); **Artio** (Real Logic) for the high-throughput inbound FIX-as-server tier and the Eurex/SIX ULL hot path.
- **Hot-path messaging**: Aeron 1.45 (open-source) with Aeron Cluster (Raft) for matching/risk/EMS replication; SBE 1.31 for binary codec; LMAX Disruptor 4.x for single-writer in-process queues.
- **Stream / event spine**: Apache Kafka 3.7 (KRaft mode) via Strimzi operator; Apache Flink 1.18 for surveillance & post-trade exactly-once stream processing; Kafka Streams for light transforms; MirrorMaker 2 for cross-region replication.
- **Clearing transport**: Apache Qpid JMS 2.5 (AMQP 1.0) for Eurex Clearing FIXML/FpML; SFTP via Apache Mina-SSHD for CRE / Common Report Engine pulls.
- **Vendor SDKs (in-process from vendor portals; mocked locally)**: `com.bloomberg:blpapi:3.x` (BLPAPI v3, EMSX, B-PIPE), `com.refinitiv.ema:ema:3.7.x` (RTSDK / EMA Java + ETA / RSSL for low-level), Bloomberg `OpenDACS`-style entitlement library, BidFX `bidfx-api:2.x` SDK (Pixie/Puffin).
- **Persistence**: PostgreSQL 16 (Aurora Global Database in cloud / self-managed in DC); QuestDB 9.x (intraday tick hot tier, 7-day retention); ClickHouse 24.x (multi-year tick research); Redis 7 (hot-state cache, FIX session cache, entitlement cache); MinIO local + AWS S3 / Azure Blob Storage with Object Lock for WORM archival; **kdb+/q drop-in path documented** but OSS path uses QuestDB+ClickHouse.
- **Event store**: Postgres + Outbox pattern + Aeron Archive (per region) with cryptographic hash chain (SHA-256 prev-hash linked) and S3 WORM mirror.
- **Schemas as contracts**: QuickFIX data dictionaries (FIX 4.4, 5.0 SP2, FIXT 1.1, plus per-venue dialects vendored), FIXML 5.0 SP2 XSDs, FpML 5.12 XSDs, SBE XML schemas, Apache Avro (Apicurio Registry), gRPC `.proto` (Protobuf), Pact for consumer-driven contracts.
- **Observability**: OpenTelemetry SDK + OTel Collector + Tempo (traces); Prometheus + AlertManager + Grafana + Mimir (metrics); Vector → Loki (ops logs) + OpenSearch 2.x (FIX archive, 7-year retention with WORM); ITRS Geneos drop-in via Toolkit-from-Prometheus; Pico Corvil drop-in via wire-tap mirror port; both commercial drop-ins documented in ADRs.
- **Secrets / PKI / mesh / identity**: OpenBao (OSS Vault fork — KV, PKI, Transit), cert-manager 1.15 + Linkerd 2.16 service mesh + SPIFFE/SPIRE workload identity, Keycloak 25 (OIDC/OAuth2/SAML).
- **Build & dev**: Gradle 8.10 (multi-project, JVM); uv 0.4 (Python workspace); go.work; Cargo workspace (Rust, optional); Tilt 0.33 + kind/k3d for inner-loop K8s; Docker Compose (compose.dev.yaml) for ancillary services not in K8s; Helm 3 + Helmfile + Kustomize for deployment composition; Terraform + Ansible for cloud (Azure) and bare-metal trading-floor.
- **CI**: GitHub Actions matrix (lint → unit → integration → conformance → performance → security → deploy-dev). Trivy + Grype + Syft (SBOM) + cosign (Sigstore) sign on each image. STAC-T1 / STAC-N1 mappings for tick-to-trade benchmarks (the full STAC audit is commercial; mappings allow internal regression tracking).
- **Testing**: JUnit 5 + AssertJ + Mockito (Java); pytest + Hypothesis (Python); jest / Vitest (TypeScript); go test + testify (Go). Testcontainers for all integration tests (Kafka, Postgres, Qpid, Redis, FIXimulator). jqwik 1.8 + Hypothesis for property-based tests. Pact for service↔service contracts. k6 + Gatling for performance (REST + sustained FIX). Chaos Mesh + Toxiproxy for resilience tests. FIXimulator (containerised) + Apache Qpid Broker-J + Refinitiv `OmmProvider` IProvider local + custom SIX MTS / Bloomberg / CFETS mocks. Esprow ETP / Greenline documented as commercial certification drop-ins for customer onboarding.

**Storage** (recap):
- OLTP / trade state: PostgreSQL 16 (Aurora Global Database for cross-region).
- Tick hot: QuestDB 9 (7-day retention, Postgres-wire compatible).
- Tick warm: ClickHouse 24 (5+ year retention, columnar).
- Tick tier-1 drop-in: kdb+/q (documented ADR; not OSS path).
- Cache: Redis 7.
- Event store: Postgres + Outbox + Aeron Archive, append-only with hash chain.
- Archival WORM: S3 / MinIO with Object Lock (RTS-24 5y, FINMA 10y where applicable).
- Analytics: Snowflake / Databricks (drop-in) + DuckDB (in-notebook).

**Testing** (recap):
- Unit: JUnit 5 + AssertJ + Mockito (Java), pytest (Python), Vitest (TS), go test + testify (Go).
- Property: jqwik (Java), Hypothesis (Python).
- Contract: Pact (consumer-driven).
- Integration: Testcontainers (per service: Kafka + Postgres + Qpid + Redis + FIXimulator).
- Conformance: FIXimulator-driven per venue, plus custom binary-protocol mocks (SIX MTS, Eurex Qpid Broker-J, Bloomberg in-process stub, Refinitiv local IProvider, CFETS-via-Tradeweb stub, software PTP grandmaster).
- Performance: JMH (codec / matching microbenchmarks), k6 (REST/gRPC load), Gatling (sustained FIX load), STAC-T1/N1 mapping scripts.
- Chaos: Chaos Mesh manifests (FIX session drop, Kafka partition isolate, PTP skew injection, Eurex AMQP broker restart, Aeron Cluster leader kill).
- Replay: captured-log replay harness for production incident reproduction.

**Target Platform**:
- **Local development**: macOS + Linux developer workstations; Docker Desktop / Colima or native containerd; kind or k3d for in-cluster development; Tilt for live-reload across 12+ services.
- **CI**: GitHub Actions Linux runners (`ubuntu-latest`); kind for integration jobs.
- **UAT**: Azure Kubernetes Service (AKS) in `westeurope` (Zurich data residency); Strimzi Kafka, Crossplane / Helm-managed Postgres (Azure Database for PostgreSQL Flexible Server), OpenSearch on AKS.
- **Production-shadow** (the depth target): four-region active-active — Zurich (Equinix ZH4/ZH5), London (LD4), New York (NY4), Tokyo (TY3). Hot-path adapters (SIX OTI, Eurex T7 ETI, BidFX Pixie, MarketAxess hot quotes) on bare-metal Solarflare/Mellanox NIC servers in the local Equinix POP with Aeron UDP over LCN; rest on AKS (one per region) with cross-region Kafka MirrorMaker 2, Aurora Global Database for Postgres, S3 cross-region replication for archival.
- **Time source**: Meinberg LANTIME M3000 grandmaster per region → boundary-clock per cabinet → `ptp4l` + `phc2sys` on hot-path NICs with hardware timestamping. Local-dev uses `mocks/ptp-grandmaster-sim/` and a `chrony` NTP container.

**Project Type**: Polyglot mono-repo with multiple deployable services and shared libraries. Treated as a single coordinated repository (Gradle multi-project + uv workspace + go.work + optional Cargo workspace) with a top-level `Makefile` / `Taskfile.yml` orchestrating the build drivers.

**Performance Goals** (verbatim from FRs / SCs):
- Hot-path tick-to-trade p99 < 100µs (target < 30µs in co-lo) — SC-010 / FR-020.
- Pre-trade risk gateway p99 < 50µs — SC-017 / FR-005c.
- Warm-path OMS↔EMS / drop-copy fan-out p99 < 5ms — FR-020.
- Cold-path Kafka end-to-end p99 < 50ms — FR-020.
- FIX parse / encode p99 < 5µs (JMH); SBE encode p99 < 100ns; Aeron IPC RTT p99 < 1µs.
- FIX session throughput > 5k msg/s/session sustained.
- 10M orders/day sustained with all latency tiers met — SC-013.
- 50M ticks/sec fan-out across consumers without back-pressure overflow — SC-014.
- 10,000 concurrent authenticated trader sessions — SC-015.
- 200 concurrent inbound FIX client sessions, isolated — SC-016.

**Constraints**:
- **Latency hierarchy is non-negotiable**: every component placed in exactly one tier (hot < 100µs / warm < 5ms / cold seconds). No mixing.
- **Time-sync (RTS-25)**: trading-server clock must be ≤ 100µs from UTC, logged at 1µs granularity. Domain code MUST never read wall-clock for regulatory timestamps.
- **Append-only audit (RTS-24)**: all OMS commands write to a hash-chained tamper-evident log; retention 5y baseline, 10y where FINMA requires.
- **Drop-copy as source-of-truth**: must reconcile against OMS and resolve disagreements in favour of drop-copy.
- **mTLS internally** (Linkerd + SPIFFE), TLS 1.3 to all external venues, certificate rotation automated with 30-day expiry alert.
- **WORM** (S3 Object Lock or equivalent) for FIX archive, FpML/FIXML confirmations, audit log, regulator submission acknowledgments.
- **No sensitive data in source control** (Vault / OpenBao + sealed-secrets only).
- **Schemas in source control alongside code**, every external message validated against a registered schema; unknown fields surfaced to ops without halting processing.
- **Aeron Cluster cannot span WAN**: per-region Aeron Cluster (Raft); cross-region replication via Kafka MirrorMaker 2 + Aurora Global DB.
- **Single-region failure tolerance**: RPO ≤ 5s, RTO ≤ 60s — FR-042c.

**Scale/Scope**:
- 17 deployable services (`apps/`), 8 shared libraries (`libs/`).
- 8 venue / vendor adapters (one per integration).
- ~61 functional requirements, ~19 measurable success criteria.
- Codebase target: ~150–250k LOC across all languages combined at full scope (estimate; not a target).
- 4-region active-active deployment with follow-the-sun book handover.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

The repository's `.specify/memory/constitution.md` is currently a **template stub** with no project principles ratified. There are therefore no constitutional gates to enforce against this plan. This is itself flagged as a follow-up: a constitution should be ratified that codifies the architectural commitments already implicit in the spec — at minimum:

1. **Hexagonal-with-venue-as-adapter** (no venue protocol details in the domain core).
2. **Latency-hierarchy discipline** (each component in exactly one tier; no mixing).
3. **Schemas-as-versioned-contracts** (every external message schema in source control; PRs that change a schema require a contract test update).
4. **Time-sync-as-first-class** (no `System.currentTimeMillis()` for regulatory timestamps; clock abstraction mandatory).
5. **Drop-copy-is-source-of-truth** (any reconciliation conflict resolves in favour of drop-copy).
6. **Append-only audit** (all commands hash-chained; PRs that touch the event store require a backwards-compatibility note).
7. **Test-first for protocol code** (every codec / state machine has a property-based test before merge).

Recommended follow-up: invoke `/speckit.constitution` after this plan to ratify the seven principles above before merging Phase 1 work. Until then, the **Constitution Check passes vacuously** (no defined gates to violate).

### Constitution v1.0.0 — per-principle gate evaluation (T337)

Constitution v1.0.0 was ratified on 2026-05-03 (`.specify/memory/constitution.md`). The plan
now passes every gate, with the following mechanical checks:

| # | Principle | Mechanical gate | Status |
|---|---|---|---|
| I | Hexagonal Adapter Discipline | ArchUnit `HexagonalArchitectureTest` (`tests/architecture`) — no venue/vendor imports in `libs/domain-model` | ✅ Passing |
| II | Latency-Hierarchy Discipline | Build-time annotation check via `LatencyTier` enum on every `VenueGatewayPort` impl + per-region `application-{r}.yml` tier tag | ✅ Passing |
| III | Schemas-as-Versioned-Contracts | `contracts/sbe/`, `contracts/iso20022/`, `contracts/fix/` all under source control; property tests over each builder (RTS-22, FinfraG, FIXML, Trax APA) | ✅ Passing |
| IV | Time-Sync as First-Class | ArchUnit `domainCodeDoesNotCallWallClock` + `tools/constitution-audit/audit.sh` Principle IV gate | ✅ Passing |
| V | Drop-Copy as Source of Truth | `apps/reconciler-service` consumes drop-copy stream; conflict-resolution unit tests | ✅ Passing |
| VI | Append-Only Audit | `libs/audit-chain/HashChainWriter` + `apps/audit-service/HashChainVerifier` daily CronJob | ✅ Passing |
| VII | Test-First for Protocol Code | Property tests for every codec (jqwik for Java, Hypothesis for Python) | ✅ Passing |

The audit script `tools/constitution-audit/audit.sh` runs quarterly and writes its findings to
`reports/constitution-audit-{YYYY-Qn}.md` for the architecture review board.

**Result**: PASS (vacuous). Re-check after Phase 1 design — also vacuous unless the constitution is ratified between now and then.

## Project Structure

### Documentation (this feature)

```text
specs/001-swiss-tms-platform/
├── plan.md              # This file
├── spec.md              # Feature specification (input)
├── research.md          # Phase 0 output — Decision/Rationale/Alternatives per stack choice
├── data-model.md        # Phase 1 output — aggregates, fields, relationships, lifecycle, schema sketches
├── quickstart.md        # Phase 1 output — clone → tilt up → first roundtrip in <10 min
├── contracts/           # Phase 1 output — port interfaces, topic schemas, REST/gRPC, FIX session contract
│   ├── README.md
│   ├── ports/
│   ├── kafka-topics/
│   ├── rest-grpc/
│   ├── fix-sessions/
│   └── reporting/
├── checklists/
│   └── requirements.md  # Spec quality checklist (created by /speckit.specify)
└── tasks.md             # Phase 2 output — created by /speckit.tasks (NOT this command)
```

### Source Code (repository root)

```text
swiss-tms-platform/
├── apps/                                     # Deployable services (one bounded context per service where practical)
│   ├── oms-service/                          # Java/Spring Boot 3 — Order Management aggregate, REST/gRPC, Outbox
│   ├── ems-service/                          # Java/Aeron Cluster + Disruptor + Artio — Execution, matching, SOR, algos
│   ├── pretrade-risk-gateway/                # Java/Aeron + Disruptor — Hot-path inbound risk checks (NEW; sell-side)
│   ├── inbound-fix-acceptor/                 # Java/Artio (high-throughput) + QuickFIX/J fallback — Sell-side FIX-as-server (NEW)
│   ├── market-data-service/                  # Java/Aeron — L1/L2 normalisation, fan-out
│   ├── reference-data-service/               # Python/FastAPI — instrument master, calendar, legal entity
│   ├── reporting-service/                    # Java/Spring Batch — RTS-22, FinfraG Art.39, Trax APA, EMIR
│   ├── surveillance-service/                 # Python + Apache Flink — market-abuse alerts (layering, spoofing)
│   ├── entitlements-service/                 # Java — DACS / Bloomberg EMRS sync, kill-switch, per-client risk profile
│   ├── audit-service/                        # Java — hash-chained audit log writer (Kafka → OpenSearch + S3 WORM)
│   ├── reconciler-service/                   # Java/Kafka Streams — drop-copy ↔ OMS join, mismatch alerts
│   ├── region-router/                        # Java — follow-the-sun book-of-business handover (NEW; multi-region)
│   ├── clearing-adapter-eurex/               # Java + Apache Qpid JMS (AMQP 1.0) — Eurex C7 FIXML/FpML
│   ├── clearing-adapter-six/                 # Java — SIX x-clear / SECOM ISO 20022
│   ├── clearing-adapter-otcc/                # Java — OTCC ↔ SHCH (Swap Connect Northbound)
│   ├── venue-adapter-six/                    # Java + Artio — STI (FIX 4.4) / OTI (OUCH+SoupBinTCP) / QTI / IMI (ITCH/MoldUDP64) / TRI (FinfraG Art.39)
│   ├── venue-adapter-eurex/                  # Java + custom SBE (T7 ETI) + QuickFIX/J fallback
│   ├── venue-adapter-bloomberg/              # Java + JNI BLPAPI v3 (refdata, mktdata, EMSX, B-PIPE, DL)
│   ├── venue-adapter-refinitiv/              # Java + EMA RTSDK (OmmConsumer / OmmProvider), ETA/RSSL, RDP REST/WS
│   ├── venue-adapter-tradeweb/               # Java + QuickFIX/J (TradeXpress dialect, AiEX rule engine)
│   ├── venue-adapter-marketaxess/            # Java + QuickFIX/J (FIXT.1.1 + FIX 5.0 SP2 + EP228 + Trax custom tags)
│   ├── venue-adapter-bidfx/                  # Java + BidFX SDK (Pixie firm-tradable, Puffin shared streaming)
│   ├── venue-adapter-cfets/                  # Python/FastAPI — proxy via Tradeweb / Bloomberg / MarketAxess
│   └── trader-ui/                            # TypeScript/React 18 + FINOS Perspective + Vite
├── libs/
│   ├── domain-model/                         # Java — Order, Execution, Instrument, Price, Quantity value objects, OrdStatus state machine, ports (VenueGatewayPort, ClearingPort, etc.)
│   ├── fix-codec/                            # Java — QuickFIX/J + Artio wrappers, JdbcStoreFactory (Postgres), spring-boot-starter integration
│   ├── fix-codec-py/                         # Python — simplefix wrappers for replay scripts and fixtures
│   ├── sbe-codec/                            # Java — SBE schemas + generated codecs (Gradle codegen task)
│   ├── fixml-codec/                          # Java — JAXB-generated from FIXML 5.0 SP2 XSDs
│   ├── fpml-codec/                           # Java — JAXB-generated from FpML 5.12 XSDs
│   ├── aeron-transport/                      # Java — Aeron Cluster bootstrap, Archive client, channel naming, OTel propagation
│   ├── kafka-transport/                      # Java — Kafka producer/consumer wrappers, Avro Apicurio integration, hot/warm/cold topic naming
│   ├── observability/                        # Java + Python — OTel SDK setup, Micrometer common, Aeron-counters Prometheus exporter
│   ├── security/                             # Java — Keycloak/OIDC, mTLS bootstrap, OpenBao client, SPIFFE attestation
│   ├── time-sync/                            # Java — MonotonicClock (PHC-aware), RegulatoryClock, no-wall-clock guards
│   ├── audit-chain/                          # Java — Hash-chained event writer (SHA-256 prev-hash linked)
│   └── pretrade-risk/                        # Java — risk-rule DSL + evaluator (used by both pretrade-risk-gateway and SOR)
├── contracts/                                # Schemas as source of truth (versioned with code)
│   ├── fix/                                  # QuickFIX dictionaries (FIX44.xml, FIX50SP2.xml, FIXT11.xml + per-venue dialects in venues/)
│   ├── fixml/                                # FIXML 5.0 SP2 XSDs (Eurex C7)
│   ├── fpml/                                 # FpML 5.12 XSDs (OTC IRS via Eurex Clearing OTC)
│   ├── sbe/                                  # SBE XML schemas (orders, executions, market data, eurex-t7)
│   ├── avro/                                 # Kafka topic schemas (Apicurio Registry)
│   ├── proto/                                # gRPC service contracts
│   ├── pact/                                 # Generated Pact files
│   ├── legal/gmra/                           # GMRA repo templates (Bond Connect Repo)
│   └── iso20022/                             # ISO 20022 templates (sese.023, sese.025 for SECOM)
├── infra/
│   ├── helm/                                 # Reusable charts (kafka, postgres, otel-collector, opensearch, …)
│   ├── helmfile/                             # Per-environment composition (dev, uat, prod-shadow-zh, prod-shadow-ld4, prod-shadow-ny4, prod-shadow-ty3)
│   ├── terraform/                            # Cloud baseline (Azure AKS, networking, observability)
│   │   ├── modules/aks-cluster/
│   │   ├── modules/networking-dmz/
│   │   ├── modules/observability-stack/
│   │   ├── modules/aurora-global-db/
│   │   ├── modules/kafka-strimzi/
│   │   └── environments/{dev,uat,prod-shadow-zh,prod-shadow-ld4,prod-shadow-ny4,prod-shadow-ty3}/
│   ├── ansible/                              # Bare-metal trading-floor playbooks (PTP, NIC tuning, Aeron tuning)
│   ├── kustomize/                            # Last-mile patches (per-region IPs, secrets selectors)
│   └── secrets/keystores/                    # Sealed-secrets references (Eurex truststore, vendor keys) — NOT raw secrets
├── ops/
│   ├── grafana/dashboards/                   # JSON dashboards (fix-session-health, tick-to-trade-latency, kafka-lag, eurex-amqp-throughput, region-failover, follow-the-sun-handover)
│   ├── prometheus/{alerts,recording-rules}/  # Alert rules, recording rules
│   ├── loki/                                 # Log pipelines
│   ├── opensearch/                           # FIX archive index templates, lifecycle policies
│   ├── runbooks/                             # Per-venue and per-failure-mode markdown runbooks
│   └── chaos/                                # Chaos Mesh experiments
├── tests/
│   ├── conformance/                          # FIX cert per venue (FIXimulator-backed + custom mocks)
│   ├── property/                             # Hypothesis (Python) + jqwik (Java) suites
│   ├── performance/                          # JMH, k6, Gatling, STAC-T1/N1 mapping scripts
│   ├── chaos/                                # Chaos Mesh manifests + verification scripts
│   ├── multi-region/                         # Cross-region failover, follow-the-sun handover, RPO/RTO validation
│   └── replay/                               # Captured production log replayers
├── mocks/
│   ├── fiximulator/                          # Containerised FIXimulator with all venue dictionaries
│   ├── eurex-amqp-broker/                    # Apache Qpid Broker-J + sample FIXML payloads
│   ├── bloomberg-stub/                       # In-process Java service mock for //blp/refdata, //blp/mktdata, //blp/emapisvc
│   ├── refinitiv-ema-provider/               # OMM IProvider on localhost:14002
│   ├── six-mts-stub/                         # SIX MTS-style harness for OUCH/SoupBinTCP and ITCH/MoldUDP64
│   ├── cfets-via-tradeweb-mock/              # CFETS proxy mock
│   └── ptp-grandmaster-sim/                  # Software PTP master for CI
├── tools/
│   ├── adr/                                  # MADR template + log4brains config
│   ├── architecture/                         # Structurizr DSL workspace (single source for C4 diagrams)
│   ├── codegen/                              # SBE code generator, JAXB code generator
│   ├── ptp-audit-report/                     # PDF generator for the annual RTS-25 audit pack
│   └── tilt/                                 # Tilt extensions
├── docs/                                     # MkDocs Material site
│   ├── mkdocs.yml
│   ├── architecture/                         # C4 context, container, component
│   ├── decisions/                            # MADR ADRs (seeds: hexagonal, aeron-vs-kafka, quickfix-vs-onixs, event-sourcing, ptp-rts25, multi-region, sell-side-inbound, …)
│   ├── runbooks/                             # Symlinked from ops/runbooks
│   ├── interview/                            # 30-min walkthrough script + hard-questions index
│   ├── algos/                                # Algo inventory with owner + approval (RTS-6/7)
│   └── glossary.md
├── .github/workflows/                        # lint, unit, integration, conformance, performance, security, release
├── settings.gradle.kts                       # Multi-project Gradle root
├── pyproject.toml                            # uv workspace root
├── go.work                                   # Go workspaces
├── Cargo.toml                                # Cargo workspace (optional Rust)
├── Tiltfile
├── compose.dev.yaml                          # Local docker-compose for ancillary services
├── Makefile                                  # Top-level driver
└── README.md
```

**Structure Decision**: This is a **polyglot mono-repo** with 17 deployable services in `apps/`, 13 shared libraries in `libs/`, 9 contract-schema directories in `contracts/`, and parallel infrastructure / ops / test / mock / tooling / documentation trees. The mono-repo decision is deliberate (mirrors UBS / Goldman / JPM internal practice and the input blueprint) and is enabled by Gradle multi-project as the primary build driver, with uv (Python), `go.work` (Go), and Cargo (Rust, optional) coordinated under a top-level `Makefile`. The four added services beyond the input blueprint — `pretrade-risk-gateway`, `inbound-fix-acceptor`, `region-router`, and `audit-service` — surface during planning to address clarifications (sell-side inbound flow, multi-region routing, audit-chain isolation).

## Complexity Tracking

> *Filled because the design adds significant scope beyond the original spec via clarifications. Each addition is justified.*

| Violation / Addition | Why Needed | Simpler Alternative Rejected Because |
|----------------------|------------|--------------------------------------|
| 17 deployable services (vs the original 12 in the input blueprint) | Sell-side inbound + multi-region clarifications added `inbound-fix-acceptor`, `pretrade-risk-gateway`, `region-router`, `audit-service`, `reconciler-service`, plus `clearing-adapter-six` and `clearing-adapter-otcc` for completeness | Folding inbound-FIX into OMS would put a hot-path component in a warm-path service and break the latency-hierarchy invariant (the spec's #2 design principle); folding pretrade-risk into the SOR would make the same mistake and prevent independent scale-out. Audit-chain has its own service to isolate the WORM-archive write from operational noise (single-responsibility for tamper evidence). |
| Four-region active-active deployment | FR-042a (clarified to four-region) — sell-side prime broker scale at 10M orders/day requires follow-the-sun coverage; LD4/NY4/TY3 are the three globally-recognised co-lo POPs after Zurich for European/US/Asian market-hours coverage | Single-region with cold standby (the simpler alternative) cannot meet the SC-018 ≤60s RTO with no order loss across a regional failure under sustained sell-side load; would also fail the geographic-scope requirement and waste co-lo proximity to LD4 venues |
| Aeron Cluster per region (rather than one global cluster) | Raft-based consensus has tight RTT bounds (~200ms WAN RTT to NY4 from Zurich would gut Aeron Cluster throughput); the spec's hot-path latency target (<100µs) is incompatible with cross-region Raft | A global Aeron Cluster would either violate latency targets or require Aeron Premium with proprietary clustering — both worse than per-region clusters with Kafka MirrorMaker for cross-region replication of the cold-path event store |
| Both QuickFIX/J **and** Artio in scope | QuickFIX/J is the standard for the long-tail of vendor sessions where 10–30k msg/s/session is more than enough and Spring-Boot-starter ergonomics matter; Artio is the only OSS Java FIX engine that can reach the SC-016 throughput on the inbound-FIX-acceptor and the Eurex/SIX hot-path adapters | Single FIX engine across the board: QuickFIX/J alone would miss the hot-path target (SC-010, SC-017); Artio alone would force a 12-week learning curve onto every routine vendor session and would lose the Spring-Boot-Actuator/Micrometer integration that QuickFIX/J's starter provides |
| Both QuestDB **and** ClickHouse for tick storage | QuestDB optimises intraday OHLCV / order-book queries (the hot tick-data path); ClickHouse handles multi-year columnar research workloads (the cold tick-data path); using QuestDB for both would cost orders of magnitude in storage at the SC-014 50M-ticks/sec scale | Single time-series database (kdb+/q) is the tier-1 production drop-in but is commercial; the OSS reference path needs both for the same coverage. ADR documents the kdb+/q production drop-in |
| Bare-metal trading-floor deployment alongside K8s | Hot-path adapters with Aeron UDP on Solarflare/Mellanox NICs cannot run on shared cloud K8s nodes and meet SC-010/SC-017; co-lo bare metal is required for the ULL components | All-K8s would fail the < 100µs hot-path target; all-bare-metal would lose the operational ergonomics and the AKS-based UAT/prod-shadow story |
| Production-shadow with hardware PTP grandmaster | RTS-25 compliance (FR-031, SC-007) at sell-side scale demands hardware time source per region; software PTP cannot stay within ≤100µs to UTC under load with HW-timestamping | Software PTP only would fail RTS-25 audit — FINMA-blocking |
