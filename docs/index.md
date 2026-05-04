# Swiss Trading & Market Support Platform

Reference mono-repo for a Swiss bank in Basel.

This documentation site is built from `docs/` and the [`specs/001-swiss-tms-platform/`](https://github.com/tobiasoberrauch/swiss-tms-platform/tree/main/specs/001-swiss-tms-platform) directory at the repository root. Use the navigation on the left.

## Quick links

- **[Spec](https://github.com/tobiasoberrauch/swiss-tms-platform/blob/main/specs/001-swiss-tms-platform/spec.md)** — the user-facing feature specification (10 user stories).
- **[Plan](https://github.com/tobiasoberrauch/swiss-tms-platform/blob/main/specs/001-swiss-tms-platform/plan.md)** — implementation plan with project structure and complexity tracking.
- **[Tasks](https://github.com/tobiasoberrauch/swiss-tms-platform/blob/main/specs/001-swiss-tms-platform/tasks.md)** — 339 implementation tasks across 16 phases.
- **[Constitution v1.0.0](https://github.com/tobiasoberrauch/swiss-tms-platform/blob/main/.specify/memory/constitution.md)** — governance and the seven principles every PR is gated against.

## The seven principles

1. **Hexagonal Adapter Discipline** — domain core has zero venue-specific protocol details.
2. **Latency-Hierarchy Discipline** — every component lives in exactly one tier (hot < 100µs / warm < 5ms / cold seconds).
3. **Schemas-as-Versioned-Contracts** (NON-NEGOTIABLE).
4. **Time-Sync as First-Class** — domain code never reads wall-clock for regulatory timestamps.
5. **Drop-Copy as Source of Truth** — reconciliation conflicts resolve in favour of drop-copy.
6. **Append-Only Audit** (NON-NEGOTIABLE).
7. **Test-First for Protocol Code** (NON-NEGOTIABLE).
