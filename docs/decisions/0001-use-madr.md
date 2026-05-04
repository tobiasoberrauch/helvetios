---
status: accepted
date: 2026-05-03
deciders: Architecture Review Board
consulted: Engineering leads
informed: All engineers
---

# Use Markdown Architectural Decision Records (MADR) for architecture decisions

## Context and Problem Statement

Architectural decisions in a polyglot mono-repo with many bounded contexts, regulatory constraints, and a four-region active-active topology need a durable, lightweight format that lives next to the code it governs. Without a shared format, "why did we choose X" answers degrade into Slack archaeology and tribal knowledge — a particularly bad failure mode for regulatory artefacts (RTS-6/7 algorithmic-trading governance).

How should we record architectural decisions for this platform?

## Decision Drivers

* Decisions must be discoverable from `docs/decisions/` and renderable in the MkDocs site.
* The format must be readable by every engineer and by the architecture review board.
* The format must support a status workflow (proposed → accepted → superseded).
* Decisions must be revisitable — the format must capture rationale, alternatives, and consequences, not just the chosen option.
* Decision authoring should not block delivery — the format must be lightweight enough for one engineer to write a decision in 30 minutes.
* Tooling support for indexing, status tracking, and site generation matters because the corpus will grow to dozens of ADRs (10 seed ADRs plus per-venue, per-regulator, per-protocol-amendment).

## Considered Options

* **Markdown ADR (MADR) — `tools/adr/template.md` + `log4brains` for site generation**.
* **Nygard-style ADR** (the original 2011 format).
* **arc42** templated documents.
* **No formal format** — informal markdown notes.

## Decision Outcome

Chosen option: "**MADR + log4brains**", because it is the most adopted lightweight ADR format with mature tooling, it interoperates cleanly with MkDocs Material, and it captures the four sections that matter for this platform: context, considered options, decision outcome, and pros/cons of each option. log4brains adds a navigable site, status badges, and search.

### Consequences

* Good, because every architectural decision (hexagonal pattern, Aeron vs Kafka, QuickFIX/J vs OnixS, multi-region active-active, sell-side inbound architecture, RTS-25 PTP stack, …) gets a numbered ADR that an auditor or interviewer can read in 5 minutes.
* Good, because the MADR template prompts engineers to articulate the alternatives — preventing the "we did X because we always did X" failure mode.
* Good, because log4brains generates an attractive ADR site for the portfolio walkthrough (US10).
* Neutral, because authors may produce ADRs of varying quality; this is mitigated by the architecture review board's amendment process per the constitution.
* Bad, because the format is markdown-only — diagrams must be embedded as Mermaid (renderable on GitHub) or PlantUML (renderable in MkDocs site).

## More Information

* MADR specification: https://adr.github.io/madr/
* log4brains: https://github.com/thomvaill/log4brains
* The MADR template lives at `tools/adr/template.md`. Copy it to `docs/decisions/0xxx-<kebab-title>.md` and fill in.
* Numbering is monotonic; never re-use a number even if an ADR is superseded.
