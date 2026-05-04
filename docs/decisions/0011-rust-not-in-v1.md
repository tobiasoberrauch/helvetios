# 0011 — Rust Not in v1

* Status: Accepted
* Date: 2026-05-04
* Deciders: Architecture Review Board

## Context and Problem Statement

Rust is fashionable in trading-platform discussions. Should the v1 of this reference platform
ship Rust services?

## Decision

No. Rust does **not** ship in v1.

The Cargo workspace (`Cargo.toml`) exists but contains no production crates. The platform's hot
path is JVM (Aeron + SBE + Artio) — the JIT, the safepoints, and the GC are tuneable enough to
hit our < 100 µs target without bringing in a second language.

Rust is appropriate **only** when a specific component proves untenable on the JVM. As of v1
nothing has proven that.

## Considered Options

1. Rewrite the FIX engine in Rust (`fefix`). Rejected: pre-1.0, "wildly unstable" per the
   project's own README, no Tier-1 production reference.
2. Rust crypto-venue adapter. Postponed to v2 if and when crypto becomes in-scope.
3. Rust hot-codecs. The SBE codegen produces sufficient Java; not worth the polyglot tax.

## Consequences

* `Cargo.toml` stays committed but inert; CI builds it as a smoke check only.
* Onboarding is one language simpler.
* If Rust is reintroduced, this ADR is superseded with an explicit performance-evidence section.

## Links

* [`Cargo.toml`](../../Cargo.toml)
* [`docs/decisions/0004-aeron-vs-kafka.md`](0004-aeron-vs-kafka.md)
