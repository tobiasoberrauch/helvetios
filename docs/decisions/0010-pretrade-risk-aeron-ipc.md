# 0010 — Pre-Trade Risk over Aeron IPC

* Status: Accepted
* Date: 2026-05-04
* Deciders: Architecture Review Board
* Constitution: Principle II (Latency-Hierarchy Discipline)

## Context and Problem Statement

Every inbound order from `inbound-fix-acceptor` must pass fat-finger / credit / kill-switch
checks BEFORE the OMS sees it. Each check is sub-microsecond logic; the network hop between
acceptor and risk gateway is the dominant cost.

## Decision

`apps/pretrade-risk-gateway` co-locates with `apps/inbound-fix-acceptor` in the same pod and
exchanges messages over **Aeron IPC** (shared memory, ~250 ns p99). The risk gateway exposes
no network port; it is reachable only via the IPC channel from the acceptor.

* Encoding: SBE (zero-copy, schema-versioned).
* Backpressure: Aeron's natural offer-failed semantics; on persistent backpressure the acceptor
  rejects new orders rather than queueing.
* Kill-switch state lives in `entitlements-service`; the risk gateway pulls a snapshot every
  100 ms and falls closed if the snapshot is stale.

## Alternatives Considered

* **gRPC over loopback** — measured ~10 µs p50; works but burns 40× the CPU vs Aeron IPC.
* **In-process method call** — rejected because it couples acceptor and risk gateway into one
  deployable, breaking the risk-team / connectivity-team ownership split.

## Consequences

* Acceptor + risk gateway must always deploy together (same Pod, same node, same NUMA).
* Both processes must agree on the SBE schema version → contract test required (Principle III).
* Aeron driver memory is sized for both participants jointly.

## Links

* [`docs/decisions/0009-fix-as-server-inbound.md`](0009-fix-as-server-inbound.md)
* [`libs/pretrade-risk/`](../../libs/pretrade-risk/)
