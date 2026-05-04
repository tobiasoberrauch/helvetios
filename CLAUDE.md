# helvetios Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-05-03

## Active Technologies

**Polyglot mono-repo** (001-swiss-tms-platform):

- **Java 21 (LTS)** + **Spring Boot 3.3** — service-plane backbone (OMS, EMS, venue / clearing adapters, reporting, entitlements, audit, reconciler, region-router, pre-trade risk gateway).
- **Python 3.12** + **FastAPI 0.110** — reference-data, surveillance (with Apache Flink 1.18), CFETS proxy adapter, fixtures.
- **TypeScript 5.4** + **React 18** + **FINOS Perspective** + **Vite** — trader UI.
- **Go 1.22** — operational tools (PTP audit reporter, conformance harness drivers).
- **Rust 1.78** — optional workspace, used only for hypothetical crypto adapter / hot codecs (not in v1 deliverables).

**Hot-path messaging**: Aeron 1.45 + SBE 1.31 + LMAX Disruptor 4 + Aeron Cluster (Raft, per-region).
**FIX engines**: QuickFIX/J 2.3.2 (standard sessions) + Artio (Real Logic) for inbound-FIX-acceptor and SIX/Eurex hot-path.
**Event spine**: Apache Kafka 3.7 (KRaft) via Strimzi + Apache Flink 1.18 + Kafka Streams + MirrorMaker 2 cross-region.
**Clearing**: Apache Qpid JMS 2.5 (AMQP 1.0) for Eurex C7; ISO 20022 for SIX SECOM.
**Persistence**: PostgreSQL 16 (Aurora Global DB), QuestDB 9.x (tick hot), ClickHouse 24.x (tick warm), Redis 7 (cache), S3 / MinIO Object Lock (WORM), kdb+/q drop-in path documented.
**Observability**: OpenTelemetry + Tempo (traces), Prometheus + Grafana + Mimir (metrics), Vector + Loki + OpenSearch 2.x (logs).
**Secrets / mesh / IdP**: OpenBao + cert-manager + Linkerd 2.16 + SPIFFE/SPIRE + Keycloak 25.
**Build / deploy**: Gradle 8.10, uv 0.4, go.work, Cargo (optional), Tilt 0.33 + kind/k3d, Helm 3 + Helmfile + Kustomize, Terraform + Ansible, GitHub Actions matrix CI.
**Testing**: JUnit 5 + AssertJ + jqwik 1.8 (Java property), pytest + Hypothesis (Python property), Vitest (TS), go test + testify, Pact, Testcontainers, k6 + Gatling (perf), JMH (microbenchmarks), Chaos Mesh + Toxiproxy.

## Project Structure

```text
swiss-tms-platform/
├── apps/                    # 17 deployable services (one per bounded context where practical)
├── libs/                    # 13 shared libraries (domain-model, fix-codec, sbe-codec, aeron-transport, observability, security, time-sync, audit-chain, pretrade-risk, ...)
├── contracts/               # Schemas as source of truth: fix/ fixml/ fpml/ sbe/ avro/ proto/ pact/ iso20022/ legal/gmra/
├── infra/                   # helm/ helmfile/ terraform/ ansible/ kustomize/
├── ops/                     # grafana/ prometheus/ loki/ opensearch/ runbooks/ chaos/
├── tests/                   # conformance/ property/ performance/ chaos/ multi-region/ replay/
├── mocks/                   # fiximulator/ eurex-amqp-broker/ bloomberg-stub/ refinitiv-ema-provider/ six-mts-stub/ cfets-via-tradeweb-mock/ ptp-grandmaster-sim/
├── tools/                   # adr/ architecture/ codegen/ ptp-audit-report/ tilt/
├── docs/                    # MkDocs Material site + ADRs + interview walkthrough
├── .github/workflows/       # lint, unit, integration, conformance, performance, security, release
└── (build / workspace files: settings.gradle.kts, pyproject.toml, go.work, Cargo.toml, Tiltfile, compose.dev.yaml, Makefile)
```

## Commands

```bash
make scaffold      # generate SBE / JAXB / Avro codecs, vendor mirrors
tilt up            # 17 services + mocks, live-reload (http://localhost:10350)
make smoke         # end-to-end smoke test (REST → SIX mock → Postgres + recon)
make test-property # jqwik + Hypothesis suites
make test-conformance  # FIXimulator-driven conformance per venue
make test-chaos    # Chaos Mesh resilience tests
make demo-client SENDER=ACME-CAPITAL TARGET=SWISSTMS PORT=9001    # simulated FIX client
make new-venue NAME=cboe   # scaffold a new venue adapter
```

## Code Style

- **Hexagonal-with-venue-as-adapter** — domain (`libs/domain-model/`) contains zero venue-specific protocol details; FIX tags / SBE templates / BLPAPI types live only in adapters.
- **Latency-tier discipline** — every component is in exactly one latency tier (hot < 100µs / warm < 5ms / cold seconds); no mixing.
- **Schemas-as-versioned-contracts** — every external message schema is in source control; schema changes require a contract test update in the same PR.
- **Time-sync-as-first-class** — domain code uses `libs/time-sync/RegulatoryClock`, never `System.currentTimeMillis()` for regulatory timestamps.
- **Drop-copy-is-source-of-truth** — reconciliation conflicts resolve in favour of the drop-copy stream.
- **Append-only audit** — every command emits a hash-chained `AuditEvent` to `audit.command.v1`.
- **Test-first for protocol code** — every codec / state machine has a property-based test before merge.
- **Standard idiomatic style per language** — Spotless / Checkstyle (Java), ruff + black (Python), eslint + prettier (TS), `go vet` + golangci-lint (Go), `cargo fmt` + `cargo clippy` (Rust).
- **No emojis in code or comments** unless explicitly requested.
- **Comments** — only when WHY is non-obvious; never narrate WHAT.

## Recent Changes

- **001-swiss-tms-platform**: Initial blueprint specification + clarifications + plan covering 17 services, 4-region active-active deployment, sell-side prime broker scale (10M orders/day, 50M ticks/sec), hexagonal venue/clearing/vendor adapters, hot-path Aeron + Artio + SBE + Disruptor, warm/cold Kafka + Flink event spine, 8 venue / vendor adapters (SIX, Eurex, Bloomberg, Refinitiv, Tradeweb, MarketAxess, BidFX, CFETS), FinfraG / RTS-22 / Trax APA / EMIR reporting, RTS-25 PTP time-sync, hash-chained audit, FIX-as-server inbound + pre-trade-risk gateway, follow-the-sun region-router, production-shadow-grade depth.

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
