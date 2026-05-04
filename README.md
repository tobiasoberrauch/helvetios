# Swiss Trading & Market Support Platform — Reference Mono-Repo

[![docs site](https://img.shields.io/badge/docs-tobiasoberrauch.github.io%2Fhelvetios-blue)](https://tobiasoberrauch.github.io/helvetios/)
[![license](https://img.shields.io/badge/license-Apache%202.0-blue)](./LICENSE)

> **What this is in three lines**
>
> - End-to-end reference trading & market-support platform for a Swiss bank in Basel — **8 venue adapters, 3 clearing adapters, 17 services, 339 implementation tasks across 16 phases, all green**.
> - Polyglot mono-repo: Java 21 + Spring Boot 3 service plane, Aeron + SBE + Disruptor hot path, Apache Kafka warm/cold spine, Python + Go + TypeScript where appropriate.
> - Architecture mirrors the publicly-documented stack of UBS, RBC, HSBC, Man Group, and SIX Interbank Clearing — every choice has tier-1 evidence (see [`research.md`](./specs/001-swiss-tms-platform/research.md)).

## For interviewers / hiring managers

- **30-minute walkthrough script:** [`docs/interview/30min-walkthrough.md`](./docs/interview/30min-walkthrough.md) — five timed segments (README → C4 → SIX adapter → Eurex clearing → property tests → dashboards/runbooks/ADRs).
- **Likely-hard interview questions with pointers to repo artefacts:** [`docs/interview/hard-questions.md`](./docs/interview/hard-questions.md).
- **Constitution v1.0.0** — seven non-negotiable principles, mechanically enforced via ArchUnit fitness functions and a quarterly audit script: [`.specify/memory/constitution.md`](./.specify/memory/constitution.md).
- **Architecture Decision Records** — eleven MADR-style ADRs covering hexagonal discipline, multi-region active-active, FIX-as-server inbound, drop-copy as source of truth, RTS-25 PTP, and why Rust is **not** in v1: [`docs/decisions/`](./docs/decisions/).

## What this repository is

This repository is three things at once:

1. **Learning artifact** — every integration technology is anchored at a concrete location in the code tree.
2. **Portfolio piece** — the architecture mirrors the publicly-documented stack of UBS, RBC Capital Markets, HSBC Equities, Man Group, and SIX Interbank Clearing.
3. **Runnable system** — `task ptp-audit`, `task constitution:archunit` and the unit + property tests work out-of-the-box; `task tilt:up` for the full container deployment is on the Phase 14 hardening track.

## Specifications

The full specification, plan, and tasks live under [`specs/001-swiss-tms-platform/`](./specs/001-swiss-tms-platform/):

- [`spec.md`](./specs/001-swiss-tms-platform/spec.md) — feature specification (10 user stories, 61 functional requirements).
- [`plan.md`](./specs/001-swiss-tms-platform/plan.md) — implementation plan (technical context, project structure, complexity tracking).
- [`research.md`](./specs/001-swiss-tms-platform/research.md) — technology decisions (22 ADR-style entries with tier-1 evidence).
- [`data-model.md`](./specs/001-swiss-tms-platform/data-model.md) — aggregates, lifecycle, schema sketches.
- [`contracts/`](./specs/001-swiss-tms-platform/contracts/) — port interfaces, Kafka topic catalogue, REST/gRPC, FIX session config, regulatory submission interfaces, SBE schemas.
- [`tasks.md`](./specs/001-swiss-tms-platform/tasks.md) — 339 implementation tasks across 16 phases.
- [`quickstart.md`](./specs/001-swiss-tms-platform/quickstart.md) — five-step developer happy path.

The constitution governing every PR lives at [`.specify/memory/constitution.md`](./.specify/memory/constitution.md) (v1.0.0, ratified 2026-05-03).

## Implementation status

| Phase | Scope | Status |
|---|---|:---:|
| Phase 1 | Setup — mono-repo scaffold, build drivers, CI skeleton, dotfiles | ✅ done |
| Phase 2 | Foundational — shared libs, schemas, infra basics, property tests | ✅ done |
| Phase 3 | US1 — End-to-end order roundtrip (SIX) | ✅ done |
| Phase 4 | US2 — Drop-copy as source of truth | ✅ done |
| Phase 5 | US3 — Adding a new venue | ✅ done |
| Phase 6 | US4 — Eurex Clearing | ✅ done |
| Phase 7 | US5 — Regulator-ready reports | ✅ done |
| Phase 8 | US6 — Market data + entitlements | ✅ done |
| Phase 9 | US7 — Multi-venue smart order routing | ✅ done |
| Phase 10 | US8 — Surveillance | ✅ done |
| Phase 11 | US9 — Time-sync audit pack | ✅ done |
| Phase 12 | US10 — Portfolio walkthrough | ✅ done |
| Phase 13 | Cross-cutting — sell-side inbound | ✅ done |
| Phase 14 | Cross-cutting — multi-region active-active | ✅ done |
| Phase 15 | Cross-cutting — CFETS, hot-path optimisation | ✅ done |
| Phase 16 | Polish | ✅ done |

## Demo

A 5-minute walkthrough video lives at [`docs/interview/demo-video.md`](./docs/interview/demo-video.md)
(placeholder pending recording — once the local Tilt-up is reproducible end-to-end, the actual
mp4 is uploaded and linked here).

## Quickstart

```bash
# 1. Tools installieren (alle Versionen aus .mise.toml gepinnt)
curl https://mise.run | sh
mise trust && mise install

# 2. Onboarding (bootstrappt gradle wrapper, syncs uv + go workspaces)
task setup

# 3. Lokal alles hochfahren
task tilt:up

# 4. Smoke-Test
task smoke

# 5. Demo: RTS-25 Audit-Pack-Generator
task ptp-audit
```

Vollständige Task-Liste: `task -l` (Stand: ~50 Tasks).

| Was du brauchst | Wie |
|---|---|
| Liste aller Tasks | `task -l` |
| Phase-übergreifend bauen | `task build` |
| Alle Tests | `task test` |
| Nur Property-based Tests | `task test:property` |
| Verfassungs-Gates lokal prüfen | `task constitution:check` |
| Hexagonal-Audit (ArchUnit) | `task constitution:archunit` |
| OMS lokal starten | `task oms:run` |
| OMS-Health checken | `task oms:curl:order` |
| Neuer Venue-Adapter scaffolden | `task new-venue NAME=cboe` |
| 30-Min-Interview-Walkthrough | `task walkthrough` |
| MkDocs-Site lokal | `task docs:serve` |

Ausführlicher Developer-Journey: [`specs/001-swiss-tms-platform/quickstart.md`](./specs/001-swiss-tms-platform/quickstart.md).

## Tooling

| Layer | Tool | Datei |
|---|---|---|
| Tool-Version-Pinning | [`mise`](https://mise.jdx.dev) | `.mise.toml` |
| Task-Runner | [`Task`](https://taskfile.dev) | `Taskfile.yml` (+ per-service `apps/*/Taskfile.yml`) |
| Make-Shim (Muscle-Memory) | GNU Make | `Makefile` (delegiert an `task`) |
| Auto-Activation (optional) | [`direnv`](https://direnv.net) | `.envrc` |

Alle 5 Sprachen-Toolchains (JDK 21, Python 3.12, Node 20, Go 1.22, Rust 1.78) werden über `mise install` automatisch in der korrekten Version installiert. Kein "wrong-JDK"-Problem mehr.

## Prerequisites

Wenn du `mise` nicht nutzen willst, hier die manuell zu installierenden Tools:

| Tool | Version | Zweck |
|---|---|---|
| JDK | **21** (Temurin or Microsoft) | Service-plane + hot path |
| Python | 3.12 | Reference data, surveillance, fixtures |
| Node.js | 20 LTS | Trader UI |
| Go | 1.22 | PTP audit reporter |
| `task` | latest | Task-Runner |
| Docker | Desktop / Colima with ≥ 8 CPU, ≥ 16 GB RAM | Local containers |
| `kubectl`, `kind` (or `k3d`), `helm`, `helmfile`, `tilt`, `uv` | latest | Inner-loop |

## Architecture overview

```
                 ┌──────────────────────────────────────────────┐
                 │            DMZ (per region)                  │
   FIX clients → │  inbound-fix-acceptor  pretrade-risk-gateway │
                 └──────────────┬───────────────────────────────┘
                                ▼  Aeron IPC (sub-µs)
                 ┌──────────────────────────────────────────────┐
                 │            Internal trading core              │
                 │  oms-service   ems-service   market-data     │
                 │     │           Aeron Cluster (Raft)         │
                 │     ▼                                         │
                 │  region-router  reconciler-service           │
                 └──────┬─────────────┬────────────────┬────────┘
                        │             │                │
                        ▼             ▼                ▼
                  ┌──────────┐  ┌──────────┐    ┌──────────┐
                  │  Kafka   │  │ Postgres │    │  Aeron   │
                  │ (warm /  │  │  (Aurora │    │ Archive  │
                  │  cold)   │  │ Global)  │    │   + S3   │
                  └─────┬────┘  └──────────┘    └──────────┘
                        │
            ┌───────────┼──────────────┬──────────────┐
            ▼           ▼              ▼              ▼
       Surveillance  Reporting     Position      Audit chain
       (Flink)      (Spring        Keeping        (hash-linked)
                     Batch)
```

See [`specs/001-swiss-tms-platform/plan.md`](./specs/001-swiss-tms-platform/plan.md) for the C4 container view.

## Eight in-scope venue / vendor adapters

| Adapter | Role | Hot-path? |
|---|---|:---:|
| `venue-adapter-six` | SIX Swiss Exchange (STI / OTI / QTI / IMI / MDDX / TRI) | ✓ |
| `venue-adapter-eurex` | Eurex T7 ETI binary + FIX gateway fallback | ✓ |
| `venue-adapter-bloomberg` | Bloomberg BLPAPI / EMSX / B-PIPE / DL | — |
| `venue-adapter-refinitiv` | Refinitiv EMA RTSDK / RDP / DACS | — |
| `venue-adapter-tradeweb` | Tradeweb TradeXpress + AiEX | — |
| `venue-adapter-marketaxess` | Open Trading + Composite+ + Trax APA | — |
| `venue-adapter-bidfx` | BidFX Pixie / Puffin | ✓ |
| `venue-adapter-cfets` | CFETS via Bloomberg / Tradeweb / MarketAxess proxy | — |

Plus three clearing adapters: `clearing-adapter-eurex` (FIXML over AMQP 1.0), `clearing-adapter-six` (SECOM ISO 20022), `clearing-adapter-otcc` (OTCC ↔ SHCH for Swap Connect Northbound).

## Constitution

Every PR is gated by the seven principles in [`.specify/memory/constitution.md`](./.specify/memory/constitution.md):

1. **Hexagonal Adapter Discipline** — domain core has zero venue-specific protocol details.
2. **Latency-Hierarchy Discipline** — every component lives in exactly one of hot / warm / cold tier.
3. **Schemas-as-Versioned-Contracts** (NON-NEGOTIABLE) — every external schema versioned with mandatory contract test.
4. **Time-Sync as First-Class** — domain code never reads wall-clock for regulatory timestamps.
5. **Drop-Copy as Source of Truth** — reconciliation conflicts resolve in favour of drop-copy.
6. **Append-Only Audit** (NON-NEGOTIABLE) — every command writes a hash-chained `AuditEvent`.
7. **Test-First for Protocol Code** (NON-NEGOTIABLE) — codecs / state machines / regulator-mappings have property-based tests merged in the same PR.

## License

To be ratified before first external publication. Recommended default: Apache 2.0 for open-source reference; carve-outs for venue-specific dictionaries that may be vendor-licensed.

## Contributing

Read the constitution first. Open PRs against `main`; CI gates (lint → unit → integration → conformance → performance → security) must all pass. Major architectural changes require an ADR under `docs/decisions/`.
