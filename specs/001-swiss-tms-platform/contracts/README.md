# Contracts Index

The `contracts/` directory in this spec mirrors the future top-level `contracts/` directory in the source repository. It is the **single source of truth** for every interface the platform exposes (or consumes) — port interfaces between domain and adapters, Kafka topic schemas, REST/gRPC service contracts, FIX session configurations, and regulatory reporting interfaces.

Versioning policy: every schema is versioned. Breaking changes require a new version number on the schema name (`v1` → `v2`); the platform supports the previous version for at least one trading day after a new version goes live.

| File | Purpose | Versioning |
|---|---|---|
| `ports/venue-gateway-port.md` | Java port interface implemented by every venue adapter (`VenueGatewayPort`) | Semver on Java module |
| `ports/clearing-port.md` | Java port for clearing-house adapters | Semver on Java module |
| `ports/vendor-adapter-port.md` | Java port for market-data vendor adapters | Semver on Java module |
| `ports/pretrade-risk-port.md` | Pre-trade risk evaluator contract | Semver on Java module |
| `ports/entitlement-port.md` | Entitlements and kill-switch lookup contract | Semver on Java module |
| `kafka-topics/topic-naming.md` | Topic naming convention (latency tier prefix, context, event, version) | Convention |
| `kafka-topics/topics.md` | Catalog of all topics with schema references | Per-topic versioned |
| `rest-grpc/oms-api.md` | OMS REST + gRPC trader-API endpoints | OpenAPI / proto versioning |
| `rest-grpc/entitlements-api.md` | Entitlements / kill-switch REST API | OpenAPI versioning |
| `rest-grpc/reporting-api.md` | Reporting service control API | OpenAPI versioning |
| `fix-sessions/inbound-acceptor-config.md` | Per-client FIX session config schema (acceptor) | YAML schema versioning |
| `fix-sessions/outbound-initiator-config.md` | Per-venue FIX session config schema (initiator) | YAML schema versioning |
| `fix-sessions/dictionaries.md` | Per-venue FIX dialect dictionaries reference | Per-venue versioned |
| `reporting/finfrag-art39.md` | FinfraG Art. 39 submission interface | TR-published schema |
| `reporting/rts22.md` | MiFID-II RTS-22 ARM submission | ARM-published schema |
| `reporting/trax-apa.md` | Trax APA trade-publication interface | EP228 versioned |
| `reporting/emir.md` | EMIR DTCC GTR / REGIS-TR interface | TR-published schema |
| `sbe/orders.xml.md` | SBE schema for internal hot-path Order messages | SBE schema id |
| `sbe/executions.xml.md` | SBE schema for internal hot-path Execution messages | SBE schema id |
| `sbe/market-data.xml.md` | SBE schema for internal hot-path market-data messages | SBE schema id |

In the source repository, the actual machine-readable artefacts live in `contracts/{fix,fixml,fpml,sbe,avro,proto,pact,iso20022,legal/gmra}/`. The files in this spec directory are human-readable summaries of each contract's shape and semantics, intended for review during planning. Generated code from these contracts (JAXB classes, SBE codecs, Avro classes, Protobuf classes) is treated as build output.
