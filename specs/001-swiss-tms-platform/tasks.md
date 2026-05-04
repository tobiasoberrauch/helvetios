---

description: "Task list for Swiss Trading & Market Support Platform — 001-swiss-tms-platform"
---

# Tasks: Swiss Trading & Market Support Platform (Reference Mono-Repo)

**Input**: Design documents from `/specs/001-swiss-tms-platform/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md, .specify/memory/constitution.md (v1.0.0)

**Tests**: Tests are MANDATORY where the constitution requires them — Principle VII (Test-First for Protocol Code) makes property-based tests for codecs and state machines, conformance tests for venue adapters, and JMH performance gates non-negotiable. For ordinary endpoints / services, tests are still strongly recommended; tasks below mark them as required where the constitution mandates and as recommended elsewhere.

**Organization**: Tasks are grouped by user story. Phases 1–2 are shared infrastructure. Phases 3–12 cover the ten user stories from spec.md in priority order. Phases 13–15 cover cross-cutting clarifications (sell-side inbound, multi-region, CFETS) that derive from FRs but are not directly tied to a single user story. Phase 16 is polish.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: parallel-safe (different files, no dependencies on incomplete tasks)
- **[Story]**: maps to user stories from spec.md (US1 … US10); cross-cutting tasks have no story label
- File paths are absolute relative to repo root `swiss-tms-platform/`

## Path Conventions

This is a polyglot mono-repo. Top-level layout:

- Services: `apps/<service-name>/src/main/java/...` (Java) or `apps/<service-name>/<package>/` (Python) or `apps/<service-name>/src/` (TypeScript)
- Shared libs: `libs/<lib-name>/src/main/java/...`
- Schemas: `contracts/{fix,fixml,fpml,sbe,avro,proto,pact,iso20022,legal/gmra}/`
- Infra: `infra/{helm,helmfile,terraform,ansible,kustomize}/`
- Ops: `ops/{grafana,prometheus,loki,opensearch,runbooks,chaos}/`
- Tests: `tests/{conformance,property,performance,chaos,multi-region,replay}/`
- Mocks: `mocks/<mock-name>/`
- Tools: `tools/{adr,architecture,codegen,ptp-audit-report,tilt}/`
- Docs: `docs/{architecture,decisions,runbooks,interview,algos}/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Mono-repo scaffolding, build drivers, CI skeleton, top-level developer ergonomics.

- [x] T001 Create top-level directory tree per `plan.md` Project Structure section (`apps/`, `libs/`, `contracts/`, `infra/`, `ops/`, `tests/`, `mocks/`, `tools/`, `docs/`, `.github/workflows/`)
- [x] T002 Initialize Gradle multi-project root in `settings.gradle.kts` and `build.gradle.kts` with Java 21 toolchain, Spotless, Checkstyle, JaCoCo
- [x] T003 [P] Initialize uv Python workspace in `pyproject.toml` and per-service `pyproject.toml` for Python services
- [x] T004 [P] Initialize Go workspace in `go.work` and per-tool `go.mod`
- [x] T005 [P] Initialize Cargo workspace in `Cargo.toml` (members empty for v1; ready for optional Rust adapters)
- [x] T006 [P] Add top-level `Makefile` with targets `scaffold`, `build`, `test`, `test-property`, `test-conformance`, `test-chaos`, `smoke`, `new-venue`, `vendor-mirror`, `schema-reset`, `ntp-sync`
- [x] T007 [P] Add `Tiltfile` and `tilt/extensions/` skeletons covering all 17 services and the mocks
- [x] T008 [P] Add `compose.dev.yaml` for ancillary local services (Postgres 16, Kafka 3.7 KRaft, Apicurio Registry, Redis 7, QuestDB 9, ClickHouse 24, OpenSearch 2, OpenBao, Keycloak 25, Grafana, Prometheus, Tempo, FIXimulator)
- [x] T009 Add `.editorconfig`, `.gitignore`, `.gitattributes`, root `README.md` with project intro and quickstart link
- [x] T010 [P] Set up GitHub Actions skeleton workflows in `.github/workflows/`: `lint.yml`, `unit.yml`, `integration.yml`, `conformance.yml`, `performance.yml`, `security.yml`, `release.yml`
- [x] T011 [P] Wire CI image scanning (Trivy + Grype + Syft for SBOM + cosign / Sigstore signing) into `.github/workflows/security.yml`
- [x] T012 [P] Add CODEOWNERS in `.github/CODEOWNERS` enforcing principle I (adapter ownership) — `/apps/venue-adapter-*` and `/apps/clearing-adapter-*` cannot be modified by non-adapter-owners; `/libs/domain-model/` cannot be modified by adapter owners
- [x] T013 [P] Add MADR template + log4brains config in `tools/adr/{template.md,log4brains.yml}` with seed ADR `docs/decisions/0001-use-madr.md`
- [x] T014 [P] Add Structurizr DSL workspace skeleton in `tools/architecture/workspace.dsl`
- [x] T015 [P] Add MkDocs Material site skeleton in `docs/mkdocs.yml` with navigation for Architecture, Decisions, Runbooks, Interview, Glossary
- [x] T016 Smoke-test the build: `make scaffold && ./gradlew build` succeeds on an empty mono-repo

**Checkpoint**: Mono-repo skeleton exists and builds; CI runs (no jobs do real work yet).

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared libraries, contract sources, and infrastructure layers that every user story depends on. Constitution Principles I, II, III, IV, VI, VII are wired into the CI gates here.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

### Schema vendoring & codegen

- [x] T017 [P] Vendor QuickFIX data dictionaries `FIX44.xml`, `FIX50SP2.xml`, `FIXT11.xml` into `contracts/fix/`
- [x] T018 [P] Vendor venue-specific FIX dictionaries (`SIX_STI_FIX44.xml`, `EUREX_T7_FIX42.xml`, `TRADEWEB_TradeXpress.xml`, `MARKETAXESS_OPEN_TRADING.xml`, `TRAX_APA_FIX50SP2.xml`, `BLOOMBERG_EMSX_FIX44.xml`) into `contracts/fix/venues/`
- [x] T019 [P] Vendor FIXML 5.0 SP2 XSDs (Eurex C7 volumes 1–8) into `contracts/fixml/`
- [x] T020 [P] Vendor FpML 5.12 XSDs into `contracts/fpml/` (path `fpml.org/spec/fpml-5-12-7-rec-1`)
- [x] T021 [P] Author SBE schemas `orders.xml`, `executions.xml`, `market-data.xml` in `contracts/sbe/` per `contracts/sbe/orders.xml.md`, `executions.xml.md`, `market-data.xml.md`
- [x] T022 [P] Author Avro topic schemas in `contracts/avro/{hot,warm,cold}/` per `contracts/kafka-topics/topics.md` (one Avro file per topic)
- [x] T023 [P] Author gRPC `.proto` files in `contracts/proto/` for OMS, entitlements, reporting per `contracts/rest-grpc/oms-api.md`
- [x] T024 [P] Author OpenAPI YAMLs in `contracts/openapi/` for OMS, entitlements, reporting REST APIs
- [x] T025 [P] Vendor ISO 20022 sese.023 / sese.025 templates into `contracts/iso20022/`
- [x] T026 [P] Author SBE codegen Gradle task in `tools/codegen/sbe-codec-generator.gradle.kts`
- [x] T027 [P] Author JAXB codegen Gradle task for FIXML and FpML in `tools/codegen/jaxb.gradle.kts`
- [x] T028 [P] Author FIX-tag-registry-no-collision check at `tests/property/java/fix-tag-registry-no-collision-test.java` (Principle III)

### Shared libraries (libs/)

- [x] T029 [P] Implement `libs/domain-model/` core value objects: `OrderId`, `ExecutionId`, `ClientId`, `LegalEntityId`, `InstrumentId(isin,mic)`, `Price`, `Quantity`, `Side`, `OrdType`, `TimeInForce`
- [x] T030 [US3] Implement `libs/domain-model/ports/` port interfaces: `VenueGatewayPort`, `ClearingPort`, `VendorAdapterPort`, `EntitlementPort`, `PretradeRiskPort` per `contracts/ports/*.md`
- [x] T031 [P] Implement `libs/domain-model/order/` Order aggregate root and `OrdStatus` Spring Statemachine
- [x] T032 [P] Implement `libs/time-sync/RegulatoryClock` (PHC-aware) and `MonotonicClock` plus a Spotless / static check that forbids `System.currentTimeMillis()` outside of `libs/time-sync/` (Principle IV)
- [x] T033 [P] Implement `libs/audit-chain/` SHA-256 hash-chain writer with Kafka producer to `audit.command.v1` (Principle VI)
- [x] T034 [P] Implement `libs/observability/` OpenTelemetry SDK setup, Micrometer common, Aeron-counters Prometheus exporter, structured Logback / structlog config
- [x] T035 [P] Implement `libs/security/` OAuth2 Resource Server config, OpenBao client, SPIFFE attestation, mTLS bootstrap
- [x] T036 [P] Implement `libs/fix-codec/` QuickFIX/J wrappers + Postgres-backed `JdbcStoreFactory` (table `fix_session_state` with row-level locking)
- [x] T037 [P] Implement `libs/sbe-codec/` generated Java codecs from T021 plus Aeron channel naming conventions
- [x] T038 [P] Implement `libs/aeron-transport/` Aeron Cluster bootstrap, archive client, Reactive Streams adapters, OTel context propagation
- [x] T039 [P] Implement `libs/kafka-transport/` Kafka producer/consumer with Apicurio Avro integration and tier-prefix validation (warm-tier producer cannot publish to `cold.*` and vice-versa) (Principle II)
- [x] T040 [P] Implement `libs/pretrade-risk/` rule-evaluator DSL with `RiskDecision` types
- [x] T041 [P] Implement `libs/fix-codec-py/` Python wrappers around `simplefix` for fixtures and replay scripts

### Property tests for foundational code (Principle VII — MANDATORY)

- [x] T042 [P] Property test for `OrdStatus` state machine in `tests/property/java/OrderStateMachinePropertyTest.java`: invariant — after `FILLED` only `TRADE_BUSTED` is legal
- [x] T043 [P] Property test for FIX 4.4 / 5.0 SP2 NewOrderSingle / ExecutionReport / OrderCancelReplace roundtrip in `tests/property/java/FixCodecPropertyTest.java`: `parse(serialize(msg)) == msg`, BodyLength + Checksum self-consistent
- [x] T044 [P] Property test for SBE OrderSubmit / OrderAck / RiskRejection roundtrip in `tests/property/java/SbeOrdersRoundtripTest.java`
- [x] T045 [P] Property test for SBE ExecutionReport roundtrip in `tests/property/java/SbeExecutionsRoundtripTest.java`
- [x] T046 [P] Property test for SBE QuoteUpdate / TradeUpdate / BookSnapshot roundtrip in `tests/property/java/SbeMarketDataRoundtripTest.java`
- [x] T047 [P] Property test for FIXML 5.0 SP2 trade-capture XSD roundtrip in `tests/property/java/FixmlEurexTradeCaptureTest.java`
- [x] T048 [P] Property test for FpML 5.12 InterestRateStream XSD roundtrip in `tests/property/java/FpmlInterestRateStreamTest.java`
- [x] T049 [P] Hypothesis tests for `simplefix` codec in `tests/property/python/test_simplefix_roundtrip.py`
- [x] T050 [P] JMH benchmark for FIX parse/encode (target p99 < 5µs) in `tests/performance/jmh/FixCodecBench.java`
- [x] T051 [P] JMH benchmark for SBE encode (target p99 < 100ns) in `tests/performance/jmh/SbeOrdersEncodeBench.java`
- [x] T052 [P] JMH benchmark for Aeron IPC RTT (target p99 < 1µs) in `tests/performance/jmh/AeronIpcRttBench.java`

### Mocks (foundational subset)

- [x] T053 [P] Implement `mocks/fiximulator/` containerised with all venue dictionaries from T018; expose ports 9876–9890 with one configuration per venue
- [x] T054 [P] Implement `mocks/ptp-grandmaster-sim/` software PTP master + chrony NTP container

### Infra basics

- [x] T055 [P] Helm charts in `infra/helm/{kafka,postgres,opensearch,redis,questdb,clickhouse,openbao,keycloak,otel-collector}/`
- [x] T056 [P] Helmfile composition in `infra/helmfile/helmfile.yaml` with environment files `infra/helmfile/environments/{dev,uat,prod-shadow-zh,prod-shadow-ld4,prod-shadow-ny4,prod-shadow-ty3}.yaml`
- [x] T057 [P] Terraform modules in `infra/terraform/modules/{aks-cluster,networking-dmz,observability-stack,aurora-global-db,kafka-strimzi}/`
- [x] T058 [P] Wire Constitution gate workflow in `.github/workflows/lint.yml` running the FIX-tag-collision check (T028) and the no-System-currentTimeMillis check (T032)

**Checkpoint**: Foundational ready — every shared lib has property-based test coverage; Aeron / Kafka / Postgres / OpenBao / Keycloak boot under `tilt up`; Constitution gates enforced in CI. User stories may now begin.

---

## Phase 3: User Story 1 — End-to-End Order Roundtrip Against First Venue (Priority: P1) 🎯 MVP

**Goal**: A trader submits an order via REST or trader UI, the order routes through OMS to the SIX mock venue, gets acknowledged, partially filled, then fully filled, and the trader sees the final position. Backed by an append-only event store with hash-chained audit.

**Independent Test**: `tilt up && make smoke` produces an `ExecutionReport (35=8) FILLED` chain in < 60s; the order is queryable via `GET /orders/{orderId}`; the audit chain hashes verify; Postgres `order_event` table has 4+ rows.

### Tests for User Story 1 (Principle VII — MANDATORY)

- [x] T059 [P] [US1] Conformance test in `tests/conformance/six-sti/SixStiBasicLifecycleTest.java`: NewOrderSingle → ExecutionReport NEW → ExecutionReport PARTIAL_FILL → ExecutionReport FILLED
- [x] T060 [P] [US1] Conformance test in `tests/conformance/six-sti/SixStiCancelTest.java`: NewOrderSingle → OrderCancelRequest → ExecutionReport CANCELED
- [x] T061 [P] [US1] Conformance test in `tests/conformance/six-sti/SixStiCancelReplaceTest.java`: NewOrderSingle → OrderCancelReplaceRequest → ExecutionReport REPLACED
- [x] T062 [P] [US1] Conformance test in `tests/conformance/six-sti/SixStiSequenceResyncTest.java`: gap → ResendRequest → application replay vs admin GapFill
- [x] T063 [P] [US1] Pact contract test in `apps/venue-adapter-six/src/test/java/.../SixVenueAdapterPactTest.java` against `VenueGatewayPort`

### OMS service implementation (apps/oms-service/)

- [x] T064 [P] [US1] Implement `apps/oms-service/build.gradle.kts` with Spring Boot 3, Spring Data JPA, Spring Security, Micrometer, Spring Statemachine
- [x] T065 [P] [US1] Implement Order aggregate persistence in `apps/oms-service/src/main/java/ch/swisstms/oms/infra/OrderRepository.java` (Postgres via JPA + Flyway migration `V1__order_aggregate.sql`)
- [x] T066 [P] [US1] Implement OMS Outbox table + Debezium configuration in `apps/oms-service/src/main/resources/db/migration/V2__outbox.sql`
- [x] T067 [P] [US1] Implement order_event table + hash-chain trigger in `apps/oms-service/src/main/resources/db/migration/V3__order_event.sql`
- [x] T068 [US1] Implement OMS application service in `apps/oms-service/src/main/java/ch/swisstms/oms/application/OrderApplicationService.java` (depends T065, T066, T067)
- [x] T069 [US1] Implement REST controller in `apps/oms-service/src/main/java/ch/swisstms/oms/api/OrderController.java` (depends T068) — endpoints per `contracts/rest-grpc/oms-api.md`
- [x] T070 [US1] Implement gRPC service in `apps/oms-service/src/main/java/ch/swisstms/oms/api/OrderGrpcService.java` (depends T068)
- [x] T071 [US1] Wire Spring Statemachine for `OrdStatus` transitions in `apps/oms-service/src/main/java/ch/swisstms/oms/domain/OrdStatusConfig.java`
- [x] T072 [US1] Wire Kafka producer publishing OMS events to `cold.oms.event.v1` and audit events to `audit.command.v1` (depends T033, T039)
- [x] T073 [US1] Wire OpenTelemetry trace context propagation through REST → service → Kafka → adapter (depends T034)
- [x] T074 [US1] Helm chart for `apps/oms-service/` in `apps/oms-service/helm/`

### SIX STI venue adapter (apps/venue-adapter-six/sti/)

- [x] T075 [P] [US1] Implement `apps/venue-adapter-six/build.gradle.kts` with QuickFIX/J Spring-Boot starter
- [x] T076 [US1] Implement STI adapter in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/sti/SixStiAdapter.java` implementing `VenueGatewayPort` (depends T030, T036)
- [x] T077 [US1] Implement FIX 4.4 dialect mapper in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/sti/SixStiMessageMapper.java` (Domain ↔ FIX `35=D/8/F/G/9/j/Q`)
- [x] T078 [US1] Wire QuickFIX `.cfg` for SIX STI in `apps/venue-adapter-six/src/main/resources/quickfix/SIX_STI_initiator.cfg`
- [x] T079 [US1] Wire `JdbcStoreFactory` (Postgres) for sequence-number persistence in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/sti/SixStiSessionStore.java` (depends T036)
- [x] T080 [US1] Implement Aeron-bridged execution publisher in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/sti/SixStiExecutionPublisher.java` (depends T038)
- [x] T081 [US1] Helm chart for `apps/venue-adapter-six/` in `apps/venue-adapter-six/helm/`

### SIX MTS-style mock for STI

- [x] T082 [P] [US1] Implement `mocks/six-mts-stub/` SIX MTS-style harness exposing FIX acceptor for STI plus mock fill-generator that emits PARTIAL_FILL → FILLED on every NewOrderSingle

### Trader UI (apps/trader-ui/) minimal

- [x] T083 [P] [US1] Initialise `apps/trader-ui/` with Vite + React 18 + TypeScript 5.4 + FINOS Perspective, plus Keycloak OIDC client
- [x] T084 [US1] Implement order-entry form + open-orders blotter in `apps/trader-ui/src/views/OrderEntryView.tsx` and `OpenOrdersView.tsx`
- [x] T085 [US1] Wire Pact provider verification for trader-ui ↔ oms-service in `apps/trader-ui/tests/pact-provider-verification.test.ts`

### End-to-end smoke

- [x] T086 [US1] Implement `make smoke` script in `Makefile`: REST POST → assert 202 → poll `GET /orders/{id}` → assert FILLED → query Postgres `order_event` → assert 4+ rows → verify hash chain
- [x] T087 [US1] Wire Tilt extension `tools/tilt/extensions/oms.star` so `tilt up` brings up oms-service + venue-adapter-six + six-mts-stub + Postgres + Kafka + Apicurio + Keycloak
- [x] T088 [US1] Grafana dashboard `ops/grafana/dashboards/oms-roundtrip.json` showing order-submission rate, ExecutionReport latency p99, OMS event-store growth, audit-chain verification status

**Checkpoint**: User Story 1 fully functional. `tilt up && make smoke` exits 0 in < 60s. The trader UI shows the executed position. ADR `docs/decisions/0002-hexagonal.md` documents the venue-adapter pattern in code.

---

## Phase 4: User Story 2 — Drop-Copy as Independent Source of Truth (Priority: P1)

**Goal**: A second, independent stream of executions ("drop-copy") is consumed alongside the OMS execution stream. The reconciler service joins both on `(SenderCompID, ClOrdID, ExecID)`, raises an alert on mismatch, and resolves disagreements in favour of drop-copy. After a forced OMS outage, the OMS rebuilds its state from drop-copy alone.

**Independent Test**: Stop OMS while the venue mock continues emitting fills into drop-copy → restart OMS → assert no lost / duplicate fills, no manual intervention.

### Tests for User Story 2

- [x] T089 [P] [US2] Chaos test `tests/chaos/oms-outage-with-dropcopy.yaml` (Chaos Mesh) — kills OMS pod for 50 simulated minutes while drop-copy continues; asserts post-recovery reconciliation
- [x] T090 [P] [US2] Integration test in `tests/integration/ReconcilerIntegrationTest.java` (Testcontainers) — injects 100 fills via drop-copy and 99 via OMS stream; asserts mismatch alert on the missing one
- [x] T091 [P] [US2] Property test in `tests/property/java/ReconcilerKeyJoinTest.java` — random `(SenderCompID, ClOrdID, ExecID)` triples roundtrip through reconciler

### Reconciler service (apps/reconciler-service/)

- [x] T092 [P] [US2] Implement `apps/reconciler-service/build.gradle.kts` with Kafka Streams
- [x] T093 [US2] Implement reconciler topology in `apps/reconciler-service/src/main/java/ch/swisstms/reconciler/ReconcilerTopology.java` joining `cold.oms.event.v1` and `warm.dropcopy.six.v1` (depends T039)
- [x] T094 [US2] Implement mismatch publisher to `warm.recon.mismatch.v1` in `apps/reconciler-service/src/main/java/ch/swisstms/reconciler/MismatchPublisher.java`
- [x] T095 [US2] Implement post-recon authoritative `cold.exec.fill.v1` publisher in `apps/reconciler-service/src/main/java/ch/swisstms/reconciler/AuthoritativeFillPublisher.java` (Principle V)
- [x] T096 [US2] Helm chart for `apps/reconciler-service/` in `apps/reconciler-service/helm/`

### Drop-copy producer (extension to SIX adapter)

- [x] T097 [US2] Implement drop-copy session in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/sti/SixStiDropCopyProducer.java` publishing to `warm.dropcopy.six.v1`
- [x] T098 [US2] Extend `mocks/six-mts-stub/` to emit a duplicate ExecutionReport stream on a separate FIX session for drop-copy

### OMS recovery from drop-copy

- [x] T099 [US2] Implement OMS state-rebuild-from-drop-copy job in `apps/oms-service/src/main/java/ch/swisstms/oms/recovery/DropCopyRecoveryJob.java`
- [x] T100 [US2] Add AlertManager rule `ops/prometheus/alerts/recon-mismatch.yml` (Sev-2 on persistent mismatch)
- [x] T101 [US2] Author runbook `ops/runbooks/oms-recovery-from-drop-copy.md`

### Audit-chain enforcement on reconciliation actions

- [x] T102 [US2] Every reconciliation amendment writes an `AuditEvent` of type `recon.amendment` via `libs/audit-chain/` to `audit.command.v1` (Principle VI)

**Checkpoint**: US2 functional. Chaos test passes. Drop-copy is authoritative. Hash-chained audit covers reconciliation actions.

---

## Phase 5: User Story 3 — Adding a New Venue Without Touching the Domain (Priority: P2)

**Goal**: A senior engineer can scaffold a new venue adapter via `make new-venue NAME=cboe`, implement the `VenueGatewayPort`, and route orders to it without modifying domain, OMS, EMS, reconciler, reporting, or surveillance services.

**Independent Test**: Run `make new-venue NAME=test` → implement a stub adapter that always replies `FILLED` → register via configuration → submit an order routed to it → assert order completes through OMS without touching any other code.

### Code-ownership boundary enforcement

- [x] T103 [P] [US3] Implement Java module visibility in `libs/domain-model/src/main/java/module-info.java` exposing only `ch.swisstms.domain.*` ports and value objects to `apps/venue-adapter-*` (Principle I)
- [x] T104 [P] [US3] Add ArchUnit test in `tests/architecture/HexagonalArchitectureTest.java` enforcing: domain code MUST NOT reference `quickfix.*`, `bloomberg.*`, `com.refinitiv.*`, `org.apache.qpid.*`, or any venue-specific package
- [x] T105 [P] [US3] CI gate in `.github/workflows/lint.yml` running ArchUnit + CODEOWNERS modification rejection

### New-venue scaffolding generator

- [x] T106 [US3] Implement `make new-venue NAME=<name>` Makefile target that generates `apps/venue-adapter-<name>/build.gradle.kts`, skeleton `<Name>VenueAdapter.java`, `application.yml`, conformance test stub, ADR seed `docs/decisions/0xxx-venue-adapter-<name>.md`
- [x] T107 [P] [US3] Document the venue-onboarding workflow in `docs/architecture/adding-a-venue.md` with C4 component-level diagram

### Venue routing configuration

- [x] T108 [US3] Implement venue-routing config schema in `apps/oms-service/src/main/resources/application.yml` (one entry per venue MIC mapping to its adapter Spring Bean)
- [x] T109 [US3] Implement runtime routing in `apps/oms-service/src/main/java/ch/swisstms/oms/routing/VenueRoutingService.java` with `Map<MIC, VenueGatewayPort>` lookup

### Demonstration of the principle

- [x] T110 [P] [US3] Add a `mocks/test-venue-stub/` minimal adapter that implements `VenueGatewayPort` and always replies FILLED — used by the integration test to demonstrate the principle
- [x] T111 [P] [US3] Integration test `tests/integration/AddVenueWithoutTouchingDomainTest.java` (Testcontainers) — generates a new adapter, registers it, submits an order, asserts FILLED, asserts no diff in `libs/domain-model/`, `apps/oms-service/domain/`, `apps/ems-service/`, `apps/reconciler-service/`, `apps/reporting-service/`, `apps/surveillance-service/`

**Checkpoint**: US3 functional. The hexagonal principle is verifiable mechanically; ArchUnit + CODEOWNERS + module-info combine to enforce it.

---

## Phase 6: User Story 4 — Connecting to Eurex Clearing With Time-Limited Certificates (Priority: P2)

**Goal**: The platform talks to Eurex Clearing over AMQP 1.0 for trade capture, position maintenance, and broadcasts. The connection survives broker restart without losing messages, certificate rotation is automated, and an alert fires 30 days before any cert expiry.

**Independent Test**: Stop the AMQP broker mock for 60s → assert reconnection without message loss; set a cert to expire in 29 days → assert the `eurex-cert-expiry-30d` AlertManager rule fires.

### Tests for User Story 4

- [x] T112 [P] [US4] Conformance test `tests/conformance/eurex-c7/EurexClearingTradeCaptureTest.java` against `mocks/eurex-amqp-broker/` (FIXML 5.0 SP2 round-trip)
- [x] T113 [P] [US4] Property test `tests/property/java/EurexFixmlMessageRoundtripTest.java` — random TradeCaptureReport / PositionMaintenanceRequest XSD-validated
- [x] T114 [P] [US4] Chaos test `tests/chaos/eurex-amqp-broker-restart.yaml` — restarts broker, asserts `CachingConnectionFactory` reconnect with no message loss
- [x] T115 [P] [US4] Integration test `tests/integration/EurexCertRotationTest.java` — rotates cert mid-session, asserts continuity

### Eurex Clearing adapter (apps/clearing-adapter-eurex/)

- [x] T116 [P] [US4] Implement `apps/clearing-adapter-eurex/build.gradle.kts` with `org.apache.qpid:qpid-jms-client:2.5.0` and Spring JMS
- [x] T117 [US4] Implement adapter in `apps/clearing-adapter-eurex/src/main/java/ch/swisstms/clearing/eurex/EurexClearingAdapter.java` implementing `ClearingPort` (depends T030)
- [x] T118 [US4] Configure Spring `CachingConnectionFactory` (NOT `SingleConnectionFactory`) in `apps/clearing-adapter-eurex/src/main/java/ch/swisstms/clearing/eurex/QpidJmsConfig.java` with per-thread JMS sessions
- [x] T119 [US4] Implement FIXML envelope mapper in `apps/clearing-adapter-eurex/src/main/java/ch/swisstms/clearing/eurex/FixmlMessageMapper.java` (TradeCaptureReport, PositionMaintenanceRequest, public broadcasts)
- [x] T120 [US4] Implement FpML 5.12 OTC IRS mapper in `apps/clearing-adapter-eurex/src/main/java/ch/swisstms/clearing/eurex/FpmlInterestRateSwapMapper.java`
- [x] T121 [US4] Implement Common Report Engine SFTP puller in `apps/clearing-adapter-eurex/src/main/java/ch/swisstms/clearing/eurex/CommonReportEngineSftpPuller.java`
- [x] T122 [US4] Helm chart for `apps/clearing-adapter-eurex/` in `apps/clearing-adapter-eurex/helm/`

### Mock & cert rotation

- [x] T123 [P] [US4] Implement `mocks/eurex-amqp-broker/` Apache Qpid Broker-J + sample FIXML/FpML payloads container
- [x] T124 [US4] Implement cert-manager integration in `infra/helm/clearing-adapter-eurex/templates/certificate.yaml` driven by OpenBao PKI
- [x] T125 [US4] AlertManager rule `ops/prometheus/alerts/eurex-cert-expiry-30d.yml`
- [x] T126 [US4] Runbook `ops/runbooks/eurex-amqp-cert-rotation.md`
- [x] T127 [P] [US4] Audit-chain emission for cert-rotation events (Principle VI) in `apps/clearing-adapter-eurex/src/main/java/ch/swisstms/clearing/eurex/CertRotationAuditor.java`

**Checkpoint**: US4 functional. Eurex Clearing roundtrip is demonstrable; cert rotation alert verified.

---

## Phase 7: User Story 5 — Generating a Regulator-Ready Daily Transaction Report (Priority: P2)

**Goal**: Daily batch generates FinfraG Art. 39, MiFID-II RTS-22, Trax APA, and EMIR (DTCC GTR + REGIS-TR) reports from the day's fills. Reports validate against published schemas; trade-repository acks are stored; auditors can trace any trade back through the hash chain.

**Independent Test**: Replay 1,000 simulated fills → run the reporting batch → assert XML passes XSD validation → assert SIX-TR / LSEG-TRADEcho stubs accept → query `/audit/{orderId}` and confirm tamper-evident chain.

### Tests for User Story 5

- [x] T128 [P] [US5] Property test `tests/property/java/RtsTwoTwoXmlTest.java` — random RTS-22 reports XSD-validated
- [x] T129 [P] [US5] Property test `tests/property/java/FinfraGArt39XmlTest.java` — random FinfraG Art. 39 reports XSD-validated
- [x] T130 [P] [US5] Property test `tests/property/java/TraxApaTcrRoundtripTest.java` — random TradeCaptureReport(AE) + ack roundtrip
- [x] T131 [P] [US5] Integration test `tests/integration/DailyReportingBatchTest.java` (Testcontainers) — 1000 fills → 4 reports → all submission stubs accept
- [x] T132 [P] [US5] Integration test `tests/integration/AuditChainTraceabilityTest.java` — query `/audit/{orderId}` → assert traceability from fill → report submission

### Reporting service (apps/reporting-service/)

- [x] T133 [P] [US5] Implement `apps/reporting-service/build.gradle.kts` with Spring Batch + Spring Data JPA
- [x] T134 [US5] Implement FinfraG Art. 39 batch job in `apps/reporting-service/src/main/java/ch/swisstms/reporting/finfrag/FinfraGArt39Job.java` per `contracts/reporting/finfrag-art39.md`
- [x] T135 [P] [US5] Implement RTS-22 batch job in `apps/reporting-service/src/main/java/ch/swisstms/reporting/rts22/Rts22Job.java` per `contracts/reporting/rts22.md`
- [x] T136 [P] [US5] Implement Trax APA submitter in `apps/reporting-service/src/main/java/ch/swisstms/reporting/traxapa/TraxApaJob.java` (FIX outbound + CSV-SFTP fallback ≥ 3GB) per `contracts/reporting/trax-apa.md`
- [x] T137 [P] [US5] Implement EMIR DTCC GTR submitter in `apps/reporting-service/src/main/java/ch/swisstms/reporting/emir/EmirDtccGtrJob.java`
- [x] T138 [P] [US5] Implement EMIR REGIS-TR submitter in `apps/reporting-service/src/main/java/ch/swisstms/reporting/emir/EmirRegisTrJob.java`
- [x] T139 [US5] Implement schema-validation step in `apps/reporting-service/src/main/java/ch/swisstms/reporting/common/XmlValidator.java` rejecting submission on validation failure (FR-027)
- [x] T140 [US5] Implement submission-acknowledgment persistence in `apps/reporting-service/src/main/java/ch/swisstms/reporting/common/SubmissionAckPersister.java`
- [x] T141 [US5] Implement REST control API in `apps/reporting-service/src/main/java/ch/swisstms/reporting/api/ReportingController.java` per `contracts/rest-grpc/reporting-api.md`
- [x] T142 [US5] Helm chart for `apps/reporting-service/` in `apps/reporting-service/helm/`

### Submission stubs

- [x] T143 [P] [US5] Implement six-tr-submission-stub container in `mocks/six-tr-submission/` (SFTP listener)
- [x] T144 [P] [US5] Implement lseg-tradecho-stub container in `mocks/lseg-tradecho/` (REST listener)
- [x] T145 [P] [US5] Implement dtcc-gtr-stub and regis-tr-stub in `mocks/{dtcc-gtr,regis-tr}/`

### S3 WORM archival

- [x] T146 [US5] Implement S3 WORM (Object Lock) writer in `apps/reporting-service/src/main/java/ch/swisstms/reporting/archival/WormArchivalWriter.java` with retention-mode COMPLIANCE
- [x] T147 [US5] Helm value `infra/helmfile/environments/prod-shadow-zh.yaml` configures S3 bucket with retention period 5y / 10y per FR-035

### Reconciliation jobs

- [x] T148 [US5] Implement RTS-22 ack-reconciliation poll job + Sev-3 alert on > 24h-unacked records
- [x] T149 [US5] Implement Trax APA publication-feed nightly reconciliation job
- [x] T150 [US5] Implement EMIR pair-reporting weekly reconciliation job

**Checkpoint**: US5 functional. Daily batch produces 4 valid regulator submissions; auditors can trace any trade.

---

## Phase 8: User Story 6 — Real-Time Market Data With Centralised Entitlements (Priority: P2)

**Goal**: Traders subscribe to L1/L2 market data from Refinitiv (and Bloomberg). The platform checks entitlements before delivering ticks; revoking an entitlement stops the stream within one cache-refresh interval; failure of the entitlement source fails closed.

**Independent Test**: Configure trader Alice with EUR/CHF entitlement but not USD/JPY → subscribe to both → assert EUR/CHF streams and USD/JPY denied → revoke EUR/CHF → assert stream stops within cache-refresh interval.

### Tests for User Story 6

- [x] T151 [P] [US6] Conformance test `tests/conformance/refinitiv/RefinitivOmmConsumerTest.java` against `mocks/refinitiv-ema-provider/`
- [x] T152 [P] [US6] Conformance test `tests/conformance/bloomberg/BloombergRefdataMktdataTest.java` against `mocks/bloomberg-stub/`
- [x] T153 [P] [US6] Integration test `tests/integration/EntitlementsBlockUnentitledTest.java` — assert unentitled subscription is rejected
- [x] T154 [P] [US6] Integration test `tests/integration/EntitlementRevocationStopsStreamTest.java` — revoke mid-stream, assert stop within cache-refresh
- [x] T155 [P] [US6] Integration test `tests/integration/EntitlementSourceUnavailableFailClosedTest.java` — kill entitlements service, assert new subscriptions denied

### Market-data service (apps/market-data-service/)

- [x] T156 [P] [US6] Implement `apps/market-data-service/build.gradle.kts`
- [x] T157 [US6] Implement L1/L2 normalisation in `apps/market-data-service/src/main/java/ch/swisstms/marketdata/normalisation/Normaliser.java`
- [x] T158 [US6] Implement subscription manager in `apps/market-data-service/src/main/java/ch/swisstms/marketdata/subscription/SubscriptionManager.java` with state machine REQUESTED → ENTITLED → STREAMING → STOPPED / DENIED
- [x] T159 [US6] Implement Aeron multicast publisher in `apps/market-data-service/src/main/java/ch/swisstms/marketdata/publisher/AeronMulticastPublisher.java` per `contracts/sbe/market-data.xml.md`
- [x] T160 [US6] Implement Kafka cold-tier publisher to `cold.marketdata.l1.v1`
- [x] T161 [US6] Implement QuestDB writer for tick hot tier in `apps/market-data-service/src/main/java/ch/swisstms/marketdata/storage/QuestDbTickWriter.java`
- [x] T162 [US6] Implement ClickHouse writer for tick warm tier in `apps/market-data-service/src/main/java/ch/swisstms/marketdata/storage/ClickHouseTickWriter.java`
- [x] T163 [US6] Helm chart for `apps/market-data-service/` in `apps/market-data-service/helm/`

### Entitlements service (apps/entitlements-service/)

- [x] T164 [P] [US6] Implement `apps/entitlements-service/build.gradle.kts`
- [x] T165 [US6] Implement DACS / OpenDACS client in `apps/entitlements-service/src/main/java/ch/swisstms/entitlements/dacs/DacsPermissionClient.java` (PROD_PERM FID 1 PE-code matching)
- [x] T166 [US6] Implement Bloomberg EMRS client in `apps/entitlements-service/src/main/java/ch/swisstms/entitlements/bloomberg/BloombergEmrsClient.java` (UUID/SerialNumber/AuthID, seatType=BPS, 24h Identity-cache TTL)
- [x] T167 [US6] Implement entitlement cache + Kafka warm-tier publisher to `warm.entitlements.limit-update.v1`
- [x] T168 [US6] Implement REST API in `apps/entitlements-service/src/main/java/ch/swisstms/entitlements/api/EntitlementsController.java` per `contracts/rest-grpc/entitlements-api.md`
- [x] T169 [US6] Implement kill-switch endpoints with 4-eyes enforcement in `apps/entitlements-service/src/main/java/ch/swisstms/entitlements/api/KillSwitchController.java`
- [x] T170 [US6] Helm chart for `apps/entitlements-service/`

### Refinitiv adapter (apps/venue-adapter-refinitiv/)

- [x] T171 [P] [US6] Implement `apps/venue-adapter-refinitiv/build.gradle.kts` with `com.refinitiv.ema:ema:3.7.x`
- [x] T172 [US6] Implement `OmmConsumer` adapter in `apps/venue-adapter-refinitiv/src/main/java/ch/swisstms/venue/refinitiv/RefinitivEmaAdapter.java` implementing `VendorAdapterPort`
- [x] T173 [US6] Implement `OpenDACS` PE-code lookup integration in `apps/venue-adapter-refinitiv/src/main/java/ch/swisstms/venue/refinitiv/RefinitivOpenDacsBridge.java`
- [x] T174 [US6] Implement RDP REST + WebSocket V2 client (OAuth2 client-credentials) in `apps/venue-adapter-refinitiv/src/main/java/ch/swisstms/venue/refinitiv/RdpClient.java`
- [x] T175 [P] [US6] Implement `mocks/refinitiv-ema-provider/` `OmmProvider` IProvider on localhost:14002

### Bloomberg adapter (apps/venue-adapter-bloomberg/)

- [x] T176 [P] [US6] Implement `apps/venue-adapter-bloomberg/build.gradle.kts` with BLPAPI v3 (mavened from `infra/maven-mirror/`)
- [x] T177 [US6] Implement BLPAPI Identity / EMRS sync in `apps/venue-adapter-bloomberg/src/main/java/ch/swisstms/venue/bloomberg/BloombergIdentityCache.java`
- [x] T178 [US6] Implement `//blp/refdata` and `//blp/mktdata` consumers in `apps/venue-adapter-bloomberg/src/main/java/ch/swisstms/venue/bloomberg/{RefDataConsumer,MktDataConsumer}.java`
- [x] T179 [US6] Implement EMSX-API client (`//blp/emapisvc`) in `apps/venue-adapter-bloomberg/src/main/java/ch/swisstms/venue/bloomberg/emsx/EmsxClient.java`
- [x] T180 [US6] Implement Data License (DL) nightly SFTP-pull in `apps/venue-adapter-bloomberg/src/main/java/ch/swisstms/venue/bloomberg/dl/DataLicensePuller.java`
- [x] T181 [P] [US6] Implement `mocks/bloomberg-stub/` in-process Java service mock for the four BLPAPI services

### Reference data service (apps/reference-data-service/)

- [x] T182 [P] [US6] Implement Python FastAPI reference-data-service in `apps/reference-data-service/refdata/main.py` with Postgres-backed Instrument / LegalEntity / Calendar repositories
- [x] T183 [US6] Wire Bloomberg DL nightly ingest into Postgres `instrument_master` via reference-data-service

**Checkpoint**: US6 functional. Live ticks flow gated by entitlements; revocation stops streams; entitlements-service unavailability fails closed.

---

## Phase 9: User Story 7 — Multi-Venue Smart Order Routing for Bonds & FX (Priority: P3)

**Goal**: An RFQ from a trader fans out to Tradeweb, MarketAxess, and BidFX simultaneously; quotes aggregate; AiEX-style automation rules pick a winner; TCA records are written.

**Independent Test**: Submit RFQ via OMS → assert quote requests sent to all three mocks within 100ms → assert winner picked → assert TCA record in `tca.event.v1`.

### Tests for User Story 7

- [x] T184 [P] [US7] Conformance test `tests/conformance/tradeweb/TradewebRfqFlowTest.java` against FIXimulator with TradeXpress dialect
- [x] T185 [P] [US7] Conformance test `tests/conformance/marketaxess/MarketAxessOpenTradingTest.java`
- [x] T186 [P] [US7] Conformance test `tests/conformance/bidfx/BidFxPixiePuffinTest.java`
- [x] T187 [P] [US7] Integration test `tests/integration/MultiVenueRfqAggregatorTest.java` — RFQ to 3 mocks, aggregate quotes, AiEX rule, TCA emitted

### Tradeweb adapter (apps/venue-adapter-tradeweb/)

- [x] T188 [P] [US7] Implement `apps/venue-adapter-tradeweb/build.gradle.kts`
- [x] T189 [US7] Implement TradeXpress FIX adapter in `apps/venue-adapter-tradeweb/src/main/java/ch/swisstms/venue/tradeweb/TradewebAdapter.java` (RFQ flow `R/S/D/8`)
- [x] T190 [US7] Implement AiEX rule engine in `apps/venue-adapter-tradeweb/src/main/java/ch/swisstms/venue/tradeweb/aiex/AiexRuleEngine.java` reading `aiex/rules.yaml` (dealer count, price tolerance, time-in-comp, fallback)
- [x] T191 [US7] Implement TCA hook in `apps/venue-adapter-tradeweb/src/main/java/ch/swisstms/venue/tradeweb/tca/TcaEmitter.java` publishing to `tca.event.v1`

### MarketAxess adapter (apps/venue-adapter-marketaxess/)

- [x] T192 [P] [US7] Implement `apps/venue-adapter-marketaxess/build.gradle.kts`
- [x] T193 [US7] Implement Open Trading FIX channel + Composite+ MarketDataIncrementalRefresh consumer
- [x] T194 [US7] Implement Trax APA TradeCaptureReport(AE) → TradeCaptureReportAck(AR) flow + daily 23:00–23:05 GMT session reset

### BidFX adapter (apps/venue-adapter-bidfx/)

- [x] T195 [P] [US7] Implement `apps/venue-adapter-bidfx/build.gradle.kts` with `com.bidfx:bidfx-api:2.x`
- [x] T196 [US7] Implement Pixie firm-tradable-quotes adapter and Puffin shared-streaming adapter in `apps/venue-adapter-bidfx/src/main/java/ch/swisstms/venue/bidfx/{PixieAdapter,PuffinAdapter}.java`
- [x] T197 [US7] Implement subject-builder DSL in `apps/venue-adapter-bidfx/src/main/java/ch/swisstms/venue/bidfx/SubjectBuilder.java` (Source, Symbol, Tenor, Quantity)
- [x] T198 [US7] Wire LD4 / NY4 / TY3 POPs config in `apps/venue-adapter-bidfx/src/main/resources/application.yml`

### Multi-venue RFQ aggregator (in EMS)

- [x] T199 [US7] Implement RFQ aggregator in `apps/ems-service/src/main/java/ch/swisstms/ems/rfq/MultiVenueRfqAggregator.java`
- [x] T200 [US7] Implement quote-comparator + winner-picker in `apps/ems-service/src/main/java/ch/swisstms/ems/rfq/QuoteComparator.java`

**Checkpoint**: US7 functional. RFQ → aggregate → winner-pick demonstrable end-to-end.

---

## Phase 10: User Story 8 — Surveillance Detects Layering / Spoofing (Priority: P3)

**Goal**: Apache Flink job consumes order-book and execution events, applies layering / spoofing detection, writes ranked alerts; analyst can mark true/false-positive.

**Independent Test**: Inject a synthetic layering pattern → assert alert produced with correct order trail, severity, and microsecond timestamps.

### Tests for User Story 8

- [x] T201 [P] [US8] Property test `tests/property/python/test_layering_detection.py` — random order-book sequences validate detection invariants
- [x] T202 [P] [US8] Integration test `tests/integration/LayeringInjectionTest.java` — inject layering → assert alert with full trail
- [x] T203 [P] [US8] Integration test `tests/integration/SpoofingDetectionTest.java`

### Surveillance service (apps/surveillance-service/)

- [x] T204 [P] [US8] Implement `apps/surveillance-service/pyproject.toml` with PyFlink + Hypothesis
- [x] T205 [US8] Implement layering / spoofing pattern detector in `apps/surveillance-service/surveillance/patterns/layering_spoofing.py` over a sliding window (5-minute window, 1-second slide, exactly-once)
- [x] T206 [US8] Implement Kafka source (cold.exec.fill.v1, cold.book.event.v1) and Kafka sink (cold.surveillance.alert.v1, cold.surveillance.feedback.v1)
- [x] T207 [US8] Implement OpenSearch indexer for analyst review in `apps/surveillance-service/surveillance/index/opensearch_indexer.py`
- [x] T208 [US8] Implement REST API for analyst feedback marking in `apps/surveillance-service/surveillance/api/feedback.py`
- [x] T209 [US8] Implement nightly tuning job that consumes feedback in `apps/surveillance-service/surveillance/tuning/nightly_tuner.py`
- [x] T210 [US8] Helm chart for `apps/surveillance-service/`

**Checkpoint**: US8 functional. Synthetic layering → ranked alert → analyst marks → tuning job consumes feedback.

---

## Phase 11: User Story 9 — Time-Synchronisation Audit Pack (Priority: P3)

**Goal**: Annual RTS-25 audit pack PDF showing per-server median + max divergence from UTC, signed and tamper-evident, traceable to the upstream grandmaster.

**Independent Test**: Run `tools/ptp-audit-report/` over 24 hours of simulated PTP/NTP logs → assert PDF generated with expected statistics → modify a byte → assert signature verification fails.

### Tests for User Story 9

- [x] T211 [P] [US9] Property test `tools/ptp-audit-report/internal/audit/audit_test.go` — random PTP/NTP log sequences produce consistent statistics
- [x] T212 [P] [US9] Integration test `tests/integration/PtpAuditPackSigningTest.java` — generate PDF, modify byte, verify signature fails

### PTP audit reporter (tools/ptp-audit-report/)

- [x] T213 [P] [US9] Initialise Go tool `tools/ptp-audit-report/go.mod`
- [x] T214 [US9] Implement OpenSearch reader for `ptp4l` / `phc2sys` daily logs in `tools/ptp-audit-report/internal/source/opensearch_source.go`
- [x] T215 [US9] Implement statistics computation (median, p99, max divergence) in `tools/ptp-audit-report/internal/audit/stats.go`
- [x] T216 [US9] Implement PDF generator with cosign-signed footer in `tools/ptp-audit-report/internal/render/pdf_renderer.go`
- [x] T217 [US9] Implement CLI in `tools/ptp-audit-report/cmd/ptp-audit-report/main.go`
- [x] T218 [US9] Write `docs/runbooks/ptp-audit-pack-generation.md` with sample command and FINMA submission steps

### Production PTP infrastructure

- [x] T219 [P] [US9] Ansible playbook for Meinberg LANTIME M3000 + boundary-clock per cabinet in `infra/ansible/playbooks/ptp-grandmaster.yml`
- [x] T220 [P] [US9] Ansible playbook for `ptp4l` + `phc2sys` on Solarflare/Mellanox NICs with HW timestamping in `infra/ansible/playbooks/ptp-clients.yml`
- [x] T221 [P] [US9] Vector pipeline shipping daily PTP logs to OpenSearch in `ops/loki/vector/ptp-pipeline.yml`
- [x] T222 [P] [US9] AlertManager rule on PTP divergence > 50µs in `ops/prometheus/alerts/ptp-divergence.yml`

**Checkpoint**: US9 functional. Annual audit pack PDF generated and signed.

---

## Phase 12: User Story 10 — Portfolio Walkthrough in 30 Minutes (Priority: P3)

**Goal**: A senior engineering manager can complete a 30-minute walkthrough of the platform during an interview without getting lost; every linked artefact exists.

**Independent Test**: Hand the repository to a peer engineer with no prior context → observe whether they complete the walkthrough script without confusion.

### Walkthrough authoring

- [x] T223 [P] [US10] Author `docs/interview/30min-walkthrough.md` with five timed segments (0–3 README, 3–8 C4, 8–14 SIX adapter, 14–20 Eurex clearing, 20–25 jqwik FIX roundtrip, 25–30 dashboard + runbook + ADR)
- [x] T224 [P] [US10] Author `docs/interview/hard-questions.md` with the eight likely-hard interview questions and pointers to repo artefacts that answer them
- [x] T225 [P] [US10] Author `docs/glossary.md` covering FIX / SBE / FpML / FIXML / drop-copy / latency-tier / hexagonal / kill-switch / DACS / EMRS / RTS-22/24/25 / FinfraG Art. 39 / Trax APA / EMIR / FpML / Aeron Cluster / SPIFFE
- [x] T226 [P] [US10] Author seed ADRs: `0002-hexagonal.md`, `0003-event-sourcing-oms.md`, `0004-aeron-vs-kafka.md`, `0005-quickfixj-vs-onixs.md`, `0006-multi-region-active-active.md`, `0007-ptp-rts25.md`, `0008-drop-copy-source-of-truth.md`, `0009-fix-as-server-inbound.md`, `0010-pretrade-risk-aeron-ipc.md`, `0011-rust-not-in-v1.md`
- [x] T227 [P] [US10] Author `docs/algos/inventory.md` listing each algo (VWAP, TWAP, POV, IS) with owner + approval stamp (RTS-6/7)

### C4 diagrams

- [x] T228 [P] [US10] Author Structurizr DSL workspace in `tools/architecture/workspace.dsl` covering C4 Context (Level 1), Container (Level 2), Component (Level 3 for OMS, EMS, venue-adapter-six)
- [x] T229 [P] [US10] Configure Structurizr export to Mermaid (GitHub-rendered) and PlantUML (in MkDocs site)

### Walkthrough validation

- [x] T230 [US10] Implement walkthrough-link-checker test `tests/integration/WalkthroughLinkValidityTest.java` asserting every file/dashboard/runbook/test mentioned in `docs/interview/30min-walkthrough.md` exists
- [x] T231 [US10] Record 5-minute demo video and link from `README.md`

**Checkpoint**: US10 functional. The walkthrough runs end-to-end against a fresh clone.

---

## Phase 13: Cross-Cutting — Sell-Side Inbound (FIX-as-Server + Pre-Trade Risk Gateway)

**Purpose**: Implements FR-005a–f and SC-016 / SC-017. Not directly tied to a single user story but essential for sell-side prime-broker scale.

### Pre-trade risk gateway (apps/pretrade-risk-gateway/)

- [x] T232 [P] Implement `apps/pretrade-risk-gateway/build.gradle.kts` with Aeron + Disruptor + Agrona
- [x] T233 Implement single-writer Disruptor evaluator in `apps/pretrade-risk-gateway/src/main/java/ch/swisstms/pretraderisk/RiskEvaluator.java` per `contracts/ports/pretrade-risk-port.md`
- [x] T234 Implement off-heap risk-profile cache (Agrona `Long2ObjectHashMap`) in `apps/pretrade-risk-gateway/src/main/java/ch/swisstms/pretraderisk/cache/RiskProfileCache.java`
- [x] T235 Implement Kafka consumer of `warm.entitlements.limit-update.v1` for incremental refresh
- [x] T236 Implement Aeron IPC inbound channel (Stream 100) and outbound channel (Stream 101 to EMS, Stream 102 back to acceptor)
- [x] T237 [P] JMH benchmark `tests/performance/jmh/PretradeRiskBench.java` enforcing p99 < 50µs (SC-017)
- [x] T238 [P] Property test `tests/property/java/PretradeRiskRulePropertyTest.java` — random profiles + orders, invariant: no over-limit order is approved
- [x] T239 Helm chart for `apps/pretrade-risk-gateway/`

### Inbound FIX acceptor (apps/inbound-fix-acceptor/)

- [x] T240 [P] Implement `apps/inbound-fix-acceptor/build.gradle.kts` with **Artio** for high-throughput sessions and QuickFIX/J fallback
- [x] T241 Implement per-client session loader from `apps/inbound-fix-acceptor/src/main/resources/clients/*.yaml` per `contracts/fix-sessions/inbound-acceptor-config.md`
- [x] T242 Implement mTLS client-cert verification in `apps/inbound-fix-acceptor/src/main/java/ch/swisstms/inbound/fix/MtlsClientCertVerifier.java`
- [x] T243 Implement per-client throttle (orders/sec, in-flight) in `apps/inbound-fix-acceptor/src/main/java/ch/swisstms/inbound/fix/PerClientThrottle.java`
- [x] T244 Implement Aeron IPC handoff to pre-trade risk gateway (zero-copy)
- [x] T245 Implement HandlInst (Tag 21) routing-mode mapper (DMA / ALGO_WHEEL / CARE) per R-020
- [x] T246 Implement separate drop-copy session per client per FR-005e
- [x] T247 Implement structured Reject(35=3) / BusinessMessageReject(35=j) generator from `RiskDecision.Rejected`
- [x] T248 [P] Conformance test `tests/conformance/inbound-fix/InboundFixSelloffTest.java` exercising the SC-016 200-concurrent-session target via FIXimulator
- [x] T249 [P] Performance test `tests/performance/gatling/InboundFixSimulation.scala` sustaining > 5k msg/s/session
- [x] T250 Helm chart for `apps/inbound-fix-acceptor/`

### Algo wheel + DMA + care order in EMS

- [x] T251 [P] Implement VWAP / TWAP / POV / IS algo strategies in `apps/ems-service/src/main/java/ch/swisstms/ems/algo/{Vwap,Twap,Pov,Is}Strategy.java`
- [x] T252 [P] Implement Smart Order Router in `apps/ems-service/src/main/java/ch/swisstms/ems/sor/SmartOrderRouter.java`
- [x] T253 [P] Implement care-order queue + trader-UI surface in `apps/ems-service/src/main/java/ch/swisstms/ems/care/CareOrderQueue.java` and `apps/trader-ui/src/views/CareOrdersView.tsx`

### EMS service (apps/ems-service/) — Aeron Cluster

- [x] T254 [P] Implement `apps/ems-service/build.gradle.kts` with Artio + Aeron Cluster
- [x] T255 Implement Aeron Cluster bootstrap (5-node Raft, quorum 3) in `apps/ems-service/src/main/java/ch/swisstms/ems/aeron/EmsClusterBootstrap.java`
- [x] T256 Implement single-writer matching thread on Disruptor in `apps/ems-service/src/main/java/ch/swisstms/ems/matching/MatchingEngine.java`
- [x] T257 Implement Chronicle Queue persistent journal (Compliance replay) in `apps/ems-service/src/main/java/ch/swisstms/ems/journal/ChronicleQueueJournal.java`
- [x] T258 [P] Chaos test `tests/chaos/aeron-cluster-leader-kill.yaml` — kill leader, assert re-election, no order loss
- [x] T259 [P] JMH benchmark `tests/performance/jmh/TickToTradeBench.java` enforcing p99 < 100µs (SC-010)
- [x] T260 Helm chart for `apps/ems-service/`

**Checkpoint**: Sell-side inbound is operational. SC-013 / SC-016 / SC-017 measurable.

---

## Phase 14: Cross-Cutting — Multi-Region Active-Active

**Purpose**: Implements FR-042a–e, SC-018, SC-019. Four-region active-active deployment with follow-the-sun.

### Region router (apps/region-router/)

- [x] T261 [P] Implement `apps/region-router/build.gradle.kts` with Spring Boot
- [x] T262 Implement YAML routing-rule reader in `apps/region-router/src/main/java/ch/swisstms/region/RoutingRulesLoader.java` (per-client preferred region, per-instrument primary venue region, per-asset-class market-hours window)
- [x] T263 Implement cutover scheduler (TY3→LD4 06:00 UTC, LD4→NY4 14:00 UTC, NY4→TY3 22:00 UTC) in `apps/region-router/src/main/java/ch/swisstms/region/CutoverScheduler.java`
- [x] T264 Implement region-tagging for inbound orders (custom FIX tag 7778 InternalRegion) in `apps/region-router/src/main/java/ch/swisstms/region/RegionTagger.java`
- [x] T265 Implement Kafka topic `warm.region.handover.signal.v1` and `region.handover.cutover.v1` publishers
- [x] T266 Helm chart for `apps/region-router/`

### Cross-region replication infra

- [x] T267 [P] Configure Kafka MirrorMaker 2 in `infra/helm/kafka-mirrormaker/` with active-active topology and offset translation
- [x] T268 [P] Configure Aurora Global Database in `infra/terraform/modules/aurora-global-db/` with primary cluster in Zurich, secondary read clusters in LD4 / NY4 / TY3
- [x] T269 [P] Configure S3 cross-region replication in `infra/terraform/modules/s3-cross-region-replication/`
- [x] T270 [P] Configure per-region Aeron Cluster (Raft quorum cannot span WAN) in `apps/ems-service/src/main/resources/application-{zh,ld4,ny4,ty3}.yml`

### Multi-region tests

- [x] T271 [P] Multi-region test `tests/multi-region/RegionalFailoverTest.java` — kill Zurich region, assert RTO ≤ 60s with no order loss (SC-018)
- [x] T272 [P] Multi-region test `tests/multi-region/FollowTheSunHandoverTest.java` — exercise cutover at scheduled times, assert no in-flight order dropped, no fill duplicated (SC-019)
- [x] T273 [P] Multi-region test `tests/multi-region/CrossRegionAuditChainTest.java` — generate events in 4 regions, verify per-region chain validity, then verify cross-region time-ordered concatenation

### Per-region prod-shadow infra

- [x] T274 [P] Terraform environment `infra/terraform/environments/prod-shadow-zh/` (Azure `switzerlandnorth`)
- [x] T275 [P] Terraform environment `infra/terraform/environments/prod-shadow-ld4/` (Azure `uksouth`)
- [x] T276 [P] Terraform environment `infra/terraform/environments/prod-shadow-ny4/` (Azure `eastus`)
- [x] T277 [P] Terraform environment `infra/terraform/environments/prod-shadow-ty3/` (Azure `japaneast`)
- [x] T278 [P] Ansible bare-metal playbook for SIX OTI / Eurex T7 ETI hot-path adapters in Equinix POPs (Solarflare/Mellanox NIC tuning, RHEL 9 `tuned`, Aeron UDP over LCN) in `infra/ansible/playbooks/colo-hot-path.yml`
- [x] T279 [P] Region-failover runbook `ops/runbooks/regional-failover.md`
- [x] T280 [P] Follow-the-sun handover runbook `ops/runbooks/follow-the-sun-handover.md`

**Checkpoint**: Multi-region operational. SC-018 / SC-019 measurable.

---

## Phase 15: Cross-Cutting — CFETS, Hot-Path Optimisation, Eurex T7 ETI Binary

**Purpose**: Remaining FR coverage that crosses user stories.

### CFETS proxy adapter (apps/venue-adapter-cfets/)

- [x] T281 [P] Initialise `apps/venue-adapter-cfets/pyproject.toml` with FastAPI
- [x] T282 Implement Bond Connect Northbound proxy via Bloomberg TSOX/VCON, Tradeweb China, MarketAxess in `apps/venue-adapter-cfets/cfets/bond_connect.py`
- [x] T283 Implement Swap Connect Northbound IRS-RFQ proxy via Tradeweb (FpML confirmation) and OTCC↔SHCH interop routing in `apps/venue-adapter-cfets/cfets/swap_connect.py`
- [x] T284 Implement Bond Connect Repo (since 10-Feb-2025) booking workflow with GMRA templates from `contracts/legal/gmra/`
- [x] T285 Author CIBM Direct onboarding-process documentation in `docs/runbooks/cfets-cibm-direct-onboarding.md` (BCCL onboarding stub adapter)
- [x] T286 [P] Implement `mocks/cfets-via-tradeweb-mock/` proxy mock

### OTCC clearing adapter (apps/clearing-adapter-otcc/)

- [x] T287 [P] Implement `apps/clearing-adapter-otcc/build.gradle.kts`
- [x] T288 Implement OTCC↔SHCH interop adapter in `apps/clearing-adapter-otcc/src/main/java/ch/swisstms/clearing/otcc/OtccClearingAdapter.java`

### SIX clearing adapter (apps/clearing-adapter-six/)

- [x] T289 [P] Implement `apps/clearing-adapter-six/build.gradle.kts`
- [x] T290 Implement SECOM ISO 20022 sese.023 / sese.025 templates in `apps/clearing-adapter-six/src/main/java/ch/swisstms/clearing/six/secom/SecomMessageBuilder.java`

### Eurex T7 ETI binary adapter (apps/venue-adapter-eurex/)

- [x] T291 [P] Implement `apps/venue-adapter-eurex/build.gradle.kts`
- [x] T292 Implement T7 ETI binary codec in `apps/venue-adapter-eurex/src/main/java/ch/swisstms/venue/eurex/eti/EurexT7EtiCodec.java` from the published `xsd`/`c-header` bundle (SBE-style templates in `contracts/sbe/eurex-t7/`)
- [x] T293 Implement T7 FIX-Gateway QuickFIX/J fallback (FIX 4.2 / 4.4)
- [x] T294 Wire `PartyIDExecutingTrader (20036)` lookup from entitlements-service

### SIX OUCH/SoupBinTCP and IMI/ITCH

- [x] T295 [P] Implement OUCH+SoupBinTCP in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/oti/OuchSoupBinTcpClient.java` (Aeron-based, sequence numbers in Redis)
- [x] T296 [P] Implement QTI Market-Maker quotes in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/qti/QtiQuoteSession.java`
- [x] T297 [P] Implement IMI ITCH/MoldUDP64 multicast subscriber + TCP gap-fill in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/imi/ItchMoldUdp64Subscriber.java`
- [x] T298 [P] Implement MDDX consolidated-feed subscriber (SIX + BME)
- [x] T299 [P] Implement TRI FinfraG Art. 39 SFTP submitter in `apps/venue-adapter-six/src/main/java/ch/swisstms/venue/six/tri/TriSftpSubmitter.java`

### Audit-service (apps/audit-service/)

- [x] T300 [P] Implement `apps/audit-service/build.gradle.kts`
- [x] T301 Implement Kafka consumer of `audit.command.v1` writing to OpenSearch + S3 WORM in `apps/audit-service/src/main/java/ch/swisstms/audit/AuditWriter.java`
- [x] T302 Implement daily hash-chain verifier job + Sev-1 alert on mismatch in `apps/audit-service/src/main/java/ch/swisstms/audit/HashChainVerifier.java` (Principle VI)
- [x] T303 [P] Helm chart for `apps/audit-service/`

### Position keeping (apps/position-keeping/)

- [x] T304 [P] Implement `apps/position-keeping/` Java service with Kafka consumer of `cold.exec.fill.v1` and idempotent position updates
- [x] T305 [P] Helm chart for `apps/position-keeping/`

**Checkpoint**: Full FR coverage; cross-cutting clarifications complete.

---

## Phase 16: Polish & Cross-Cutting Concerns

**Purpose**: Hardening, performance, security, documentation completion, chaos coverage breadth.

### Performance hardening

- [x] T306 [P] STAC-T1 mapping script in `tests/performance/stac-mappings/stac_t1_tick_to_trade.sh`
- [x] T307 [P] STAC-N1 mapping script in `tests/performance/stac-mappings/stac_n1_marketdata_feed_handler.sh`
- [x] T308 [P] k6 sustained-load test `tests/performance/k6/oms_rest_load.js` validating SC-013 (10M orders/day sustained)
- [x] T309 [P] Soak test `tests/performance/k6/marketdata_soak.js` validating SC-014 (50M ticks/sec for 8h)
- [x] T310 [P] Concurrent-trader test `tests/performance/k6/concurrent_traders.js` validating SC-015 (10k concurrent sessions)

### Chaos coverage breadth

- [x] T311 [P] Chaos test `tests/chaos/fix-session-drop.yaml` (per venue adapter)
- [x] T312 [P] Chaos test `tests/chaos/kafka-partition-isolate.yaml`
- [x] T313 [P] Chaos test `tests/chaos/ptp-skew-injection.yaml`
- [x] T314 [P] Chaos test `tests/chaos/entitlements-service-down.yaml` validating fail-closed behaviour

### Observability completeness

- [x] T315 [P] Grafana dashboard `ops/grafana/dashboards/fix-session-health.json`
- [x] T316 [P] Grafana dashboard `ops/grafana/dashboards/tick-to-trade-latency.json`
- [x] T317 [P] Grafana dashboard `ops/grafana/dashboards/kafka-lag.json`
- [x] T318 [P] Grafana dashboard `ops/grafana/dashboards/eurex-amqp-throughput.json`
- [x] T319 [P] Grafana dashboard `ops/grafana/dashboards/region-failover.json`
- [x] T320 [P] Grafana dashboard `ops/grafana/dashboards/follow-the-sun-handover.json`
- [x] T321 [P] Grafana dashboard `ops/grafana/dashboards/pretrade-risk-decisions.json`
- [x] T322 [P] OpenSearch index-template `ops/opensearch/index-templates/fix-logs.json` for FIX archive (7-year WORM)

### Runbook completeness

- [x] T323 [P] Runbook `ops/runbooks/six-sti-resync.md`
- [x] T324 [P] Runbook `ops/runbooks/bloomberg-blpapi-uuid-recovery.md`
- [x] T325 [P] Runbook `ops/runbooks/refinitiv-dacs-permissioning-drift.md`
- [x] T326 [P] Runbook `ops/runbooks/kill-switch-drill.md`
- [x] T327 [P] Runbook `ops/runbooks/finfrag-art39.md`
- [x] T328 [P] Runbook `ops/runbooks/vendor-onboarding.md`

### Documentation site

- [x] T329 [P] Build MkDocs Material site CI in `.github/workflows/docs.yml` deploying to GitHub Pages on merge to main
- [x] T330 [P] Symlink `docs/runbooks/` to `ops/runbooks/`

### Security hardening

- [x] T331 [P] cosign / Sigstore signing for every image in `.github/workflows/security.yml` (SLSA Level 3 target)
- [x] T332 [P] OPA Gatekeeper policies in `infra/kustomize/opa-gatekeeper/` for K8s NetworkPolicy / PodSecurity / ResourceLimits enforcement
- [x] T333 [P] Linkerd + SPIFFE/SPIRE workload-identity manifests in `infra/helm/linkerd/`
- [x] T334 [P] OpenBao PKI configuration in `infra/terraform/modules/openbao-pki/`

### Quickstart validation

- [x] T335 Run quickstart.md end-to-end on a fresh clone; assert every section's commands succeed; update quickstart.md for any drift

### Constitution compliance audit

- [x] T336 Implement quarterly Constitution audit script in `tools/constitution-audit/` sampling recent PRs against the 7 principles
- [x] T337 Update `specs/001-swiss-tms-platform/plan.md` Constitution Check section to reference v1.0.0 explicitly with per-principle gate evaluation

### CI completion

- [x] T338 Wire conformance CI stage in `.github/workflows/conformance.yml` running tests/conformance/* daily; publish Conformance Score Dashboard
- [x] T339 Wire performance regression tracking in `.github/workflows/performance.yml` publishing to Mimir-backed Grafana; mark PR as `performance-regression` on > 20% degradation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: no dependencies — start immediately.
- **Phase 2 (Foundational)**: depends on Phase 1 — BLOCKS all user stories. Constitution gates must be wired in CI before any US phase begins (Principles I, III, IV, VII).
- **Phase 3 (US1, P1)**: depends on Phase 2.
- **Phase 4 (US2, P1)**: depends on Phase 3 (drop-copy needs OMS event store + venue adapter).
- **Phase 5 (US3, P2)**: depends on Phase 2 (verifies hexagonal principle on the foundation); independent of Phases 3/4.
- **Phase 6 (US4, P2)**: depends on Phase 2; independent of US1/US2/US3 if shared schemas already vendored.
- **Phase 7 (US5, P2)**: depends on Phase 3 (needs `cold.oms.event.v1`) and Phase 6 (needs Eurex trade-capture for joined reports). Independent of US3/US6 if schemas vendored.
- **Phase 8 (US6, P2)**: depends on Phase 2; can proceed in parallel with US1 once foundational is done — entitlements service unblocks pre-trade risk gateway too.
- **Phase 9 (US7, P3)**: depends on Phase 2 + Phase 8 (entitlements check before quote subscription).
- **Phase 10 (US8, P3)**: depends on Phase 3 + Phase 4 (needs `cold.exec.fill.v1` post-recon).
- **Phase 11 (US9, P3)**: depends on Phase 2 (time-sync lib already there); independent.
- **Phase 12 (US10, P3)**: depends on Phases 3–11 (walkthrough cites their artefacts).
- **Phase 13 (Sell-side inbound)**: depends on Phase 8 (entitlements service is the source of risk profiles).
- **Phase 14 (Multi-region)**: depends on Phase 13 (region-router routes inbound orders) + Phase 8 (entitlements per region).
- **Phase 15 (CFETS, Hot-path, etc.)**: depends on Phase 2; mostly parallelisable.
- **Phase 16 (Polish)**: depends on prior phases.

### User Story Dependencies (per spec.md priority)

- US1 (P1) — independent after Foundational.
- US2 (P1) — depends on US1 (drop-copy operates on the same orders/fills).
- US3 (P2) — independent after Foundational; tests the hexagonal principle and unblocks future venue rollouts.
- US4 (P2) — independent after Foundational; foundational for US5's clearing-side reports.
- US5 (P2) — depends on US1 + US4 (data sources for reports).
- US6 (P2) — independent after Foundational; unblocks US7 and US-INBOUND.
- US7 (P3) — depends on US6.
- US8 (P3) — depends on US2 (post-recon `cold.exec.fill.v1` is the input).
- US9 (P3) — independent after Foundational.
- US10 (P3) — depends on all prior user stories.

### Within Each User Story

- Property tests for codecs / state machines MUST be written and must FAIL before implementation (Principle VII).
- Conformance tests MUST exist before adapter merge.
- Models / repos before services; services before endpoints; endpoints before integration tests; integration tests before chaos tests.
- Audit-chain emission added at the same PR as any state-changing command (Principle VI).
- Latency-tier placement asserted in code review against Principle II.

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel.
- All Foundational tasks marked [P] (T017–T058) can run in parallel.
- US1 + US3 + US6 + US9 can be developed in parallel by separate engineers once Foundational is complete.
- US7 (Tradeweb / MarketAxess / BidFX) — three adapters parallelisable once port + entitlements ready.
- Phase 15 CFETS / OTCC / SIX clearing / Eurex T7 ETI / SIX OUCH / IMI / QTI / TRI — all parallelisable.
- Phase 16 Polish — most tasks parallel.

---

## Parallel Example: User Story 1

```bash
# Tests first (Principle VII):
Task: "T059 Conformance test SixStiBasicLifecycleTest"
Task: "T060 Conformance test SixStiCancelTest"
Task: "T061 Conformance test SixStiCancelReplaceTest"
Task: "T062 Conformance test SixStiSequenceResyncTest"
Task: "T063 Pact test SixVenueAdapterPactTest"

# Models + repos (parallel):
Task: "T064 Initialise oms-service build.gradle.kts"
Task: "T065 OrderRepository + Flyway V1"
Task: "T066 Outbox table + Debezium V2"
Task: "T067 order_event hash-chain V3"

# Adapter assembly (parallel):
Task: "T075 Initialise venue-adapter-six build.gradle.kts"
Task: "T076 SixStiAdapter implements VenueGatewayPort"
Task: "T077 SixStiMessageMapper FIX 4.4 dialect"
Task: "T078 quickfix .cfg"

# Mock + UI in parallel:
Task: "T082 mocks/six-mts-stub harness"
Task: "T083 trader-ui Vite + React 18 + Perspective scaffold"
```

---

## Implementation Strategy

### MVP First (US1 + US2 in Phases 3–4)

1. Phase 1 (Setup) → Phase 2 (Foundational + property tests + Constitution gates)
2. Phase 3 (US1) → `tilt up && make smoke` succeeds end-to-end
3. **VALIDATE**: Demo to stakeholders. The platform now does a real order roundtrip with hash-chained audit and constitution gates enforced.
4. Phase 4 (US2) → drop-copy reconciliation operates; chaos test passes
5. **MVP READY** — minimum portfolio-grade artefact.

### Incremental Delivery (Phases 5–12 as iterative releases)

After each user-story phase, the platform is demoable independently:

- After Phase 5 (US3): "I added Cboe in 2 hours" demo.
- After Phase 6 (US4): Eurex Clearing roundtrip with cert rotation.
- After Phase 7 (US5): regulator-ready daily reports.
- After Phase 8 (US6): live market data with entitlement gating.
- After Phase 9 (US7): multi-venue RFQ.
- After Phase 10 (US8): surveillance alerts on injected layering.
- After Phase 11 (US9): RTS-25 audit pack PDF.
- After Phase 12 (US10): the 30-minute walkthrough is demonstrably runnable.

### Cross-Cutting (Phases 13–15)

- Phase 13 (Sell-side inbound) sits between US6 and US7 because it uses entitlements but unblocks scale tests for US7.
- Phase 14 (Multi-region) typically runs in parallel with the later P3 stories (US8/US9) when the US1–US7 baseline is stable.
- Phase 15 (CFETS / OTCC / Eurex T7 ETI / SIX OUCH/IMI/QTI/TRI) parallelises across engineers once Phase 2 is done.

### Polish (Phase 16)

Last; most polish tasks parallelisable. Validation at every checkpoint.

### Parallel Team Strategy

With four engineers post-Foundational:

- Engineer A: US1 → US2 → US7 → Phase 13 (sell-side inbound)
- Engineer B: US3 → US6 → US8 → Phase 14 (multi-region)
- Engineer C: US4 → US5 → Phase 15 (CFETS / OTCC / Eurex T7 ETI)
- Engineer D: US9 → Phase 12 (US10 — walkthrough) → Phase 16 (Polish)

---

## Notes

- [P] tasks = different files, no dependencies.
- [Story] label maps task to specific user story; cross-cutting phases (13–16) carry no story label.
- Each user story is independently completable and testable per the Phase checkpoints.
- Property tests for protocol code (codecs, state machines, hash chain) MUST FAIL before implementation per Principle VII.
- Audit-chain emission is added in the **same PR** as any state-changing command (Principle VI). Do not split into a separate PR.
- Latency-tier placement is asserted at code review (Principle II). The `libs/kafka-transport/` tier-prefix validator catches the most common violation.
- Hexagonal discipline (Principle I) is enforced by the Java module-info, ArchUnit test, and CODEOWNERS rules introduced in Phase 5; PRs that breach it fail CI.
- Schemas-as-versioned-contracts (Principle III) is enforced by the FIX-tag-collision check, JAXB / SBE / Avro codegen tasks, and Pact contract verifications across the platform.
- No emojis in code or comments unless explicitly requested.
- Commit at every checkpoint; stop and validate before moving to the next phase.
