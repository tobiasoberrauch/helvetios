# 0006 — Multi-Region Active-Active

* Status: Accepted
* Date: 2026-05-04
* Deciders: Architecture Review Board
* Technical story: [`specs/001-swiss-tms-platform/`](../../specs/001-swiss-tms-platform/) — clarification 4

## Context and Problem Statement

The platform must keep trading and post-trade reporting up across the regulatory zones we serve
— Switzerland (ZH), London (LD4), New York (NY4), Tokyo (TY3). Outages caused by a single
DC failure are unacceptable; FINMA expects the firm to demonstrate continuous operation.

## Decision Drivers

* No single region is allowed to be a single point of failure.
* Cross-region order writes must be visible to every region within seconds, not minutes.
* Reconciliation jobs run per region but must agree on a single global state.
* Failover MUST NOT require manual DBA intervention.

## Considered Options

1. **Active-passive** with primary in Zurich and warm standby elsewhere.
2. **Active-active across all four regions** — read-write everywhere.
3. **Active-active across two regions** (ZH + LD4) with read-only replicas in NY4 / TY3.

## Decision

Option **2 — active-active in all four regions**.

* Postgres: Aurora Global DB (write-anywhere) for OMS state.
* Kafka: per-region cluster + MirrorMaker 2 for cross-region topic mirroring.
* Aeron Cluster: per-region (Raft within region; never across).
* `apps/region-router` orchestrates traffic + cutover. Drop-copy is the source of truth
  (Constitution V) — reconciliation conflicts always favour the upstream venue feed.

## Consequences

* Higher cross-region replication cost (mostly Kafka mirroring).
* Aurora Global DB requires careful schema versioning — the entire global cluster runs the same
  schema at any moment.
* Recovery point objective (RPO) ≤ 1 second for OMS state, ≤ 5 seconds for cold-tier topics.
* Phase 14 implements the actual cutover machinery; Phase 16 hardens with chaos-mesh drills.

## Links

* [`docs/decisions/0008-drop-copy-source-of-truth.md`](0008-drop-copy-source-of-truth.md)
* [`apps/region-router/`](../../apps/region-router/)
