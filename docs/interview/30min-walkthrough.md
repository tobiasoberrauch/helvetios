# 30-Minuten-Walkthrough für Senior-Engineering-Manager

**Zielpublikum**: Hiring Manager / Senior EM bei UBS, Julius Bär,
Pictet, Swissquote, EFG, Cembra. **Lesedauer**: 30 Min — strikt
einzuhalten.

## Vorbereitung (vor dem Termin)

```bash
git clone <repo> && cd helvetios
mise install                       # einmalig — JDK 21, Python 3.12, Go 1.22, Node 20
task build                         # alle 380 Gradle-Tasks grün
task test                          # Java + Python + Go Tests
task constitution:archunit         # 9 ArchUnit-Fitness-Functions grün
task ptp-audit                     # erzeugt /tmp/audit.txt + /tmp/audit.pdf
```

Alle vier Schritte müssen **grün** durchlaufen — sonst NICHT in die Demo gehen.

> **Stand des Live-Pfads:** `task tilt:up` (alle 17 Container hochfahren) ist
> auf der Phase-14-Hardening-Liste; bis dahin laufen die Demos in dieser
> Reihenfolge gegen das lokal gebaute Repo, nicht gegen Container.

---

## [00:00–03:00] README + Tier-1-Evidenz (3 Min)

> Öffne `README.md`. Zeige drei Dinge:
>
> 1. **Industry-Expert-Empfehlung** — Stack-Choice mit publik
>    dokumentierter Tier-1-Evidenz (`UBS`, `RBC`, `HSBC`, `Man Group`,
>    `SIX`).
> 2. **8 Venue-Adapter + 3 Clearing-Adapter** als Tabelle — sofort
>    sichtbar wer in scope ist.
> 3. **Verfassung mit 7 Prinzipien** — `.specify/memory/constitution.md`
>    als ratifiziertes Governance-Dokument.

**Hauptbotschaft**: "Ich habe nicht aus Tutorials geraten, ich habe
recherchiert was Tier-1-Banken tatsächlich publizieren."

---

## [03:00–08:00] C4 Container Diagram (5 Min)

> Öffne `docs/architecture/containers.md`. Zeige das Mermaid-Diagramm.
>
> 1. **DMZ-Zone** mit den 8 Venue-Adaptern + 3 Clearing-Adaptern.
> 2. **Internal Trading Core** — OMS, EMS, Market-Data, Reference-Data,
>    Entitlements, Reconciler, Region-Router, Pre-Trade-Risk.
> 3. **Async / Compliance** — Reporting, Surveillance, Audit, Position-Keeping.
> 4. **Latency-Hierarchie** — Aeron IPC (hot) / Kafka (warm/cold) klar
>    getrennt.

**Hauptbotschaft**: "Hexagonal-mit-Venue-als-Adapter ist nicht nur Buzzword,
es ist im Verzeichnisbaum sichtbar — `apps/venue-adapter-*` sind alle
hinter dem gleichen `VenueGatewayPort`."

---

## [08:00–14:00] Venue-Adapter live (6 Min)

> Öffne `apps/venue-adapter-six/`.
>
> 1. `SixStiAdapter.java` — implementiert `VenueGatewayPort`, **keine**
>    quickfix-Imports im Domain-Code.
> 2. `SixStiMessageMapper.java` — alle FIX-Tags leben hier; Domain-Side
>    sieht nur `Order` + `ExecutionReport`.
> 3. `contracts/fix/venues/SIX_STI_FIX44.xml` — schemas-as-versioned-contracts.
> 4. **Live-Frage**: "Wenn ich morgen Cboe Europe anbinden müsste?"
>    → `make new-venue NAME=cboe` → demonstrativ ein neues Verzeichnis,
>    `HexagonalArchitectureTest` blockiert das Hinzufügen sobald jemand
>    versucht, die Domain anzufassen.

**Hauptbotschaft**: "Ports und Adapter sind hier mechanisch erzwungen, nicht
nur dokumentiert. Der ArchUnit-Test in CI wird rot, wenn jemand das
Hexagonal-Prinzip bricht."

---

## [14:00–20:00] Eurex Clearing AMQP + Cert Rotation (6 Min)

> Öffne `apps/clearing-adapter-eurex/`.
>
> 1. `QpidJmsConfig.java` — **`CachingConnectionFactory` (NICHT
>    `SingleConnectionFactory`)** — kommentiert, warum das wichtig ist
>    (Eurex-spezifische Pitfall).
> 2. `EurexClearingAdapter.java` — implementiert `ClearingPort`, FIXML-Mapping.
> 3. `CertRotationAuditor.java` — daily JKS-Check, Audit-Chain-Eintrag bei
>    cert-expiry < 30 Tagen.
> 4. `ops/runbooks/eurex-amqp-cert-rotation.md` — vollständiges Runbook
>    inkl. Notfall-Rotation, FINMA-Audit-Anforderungen.

**Hauptbotschaft**: "Die regulatorischen Eigenheiten der Eurex-CA-Rotation
(jährlich September) sind im Code, im Alert, und im Runbook — nicht in
einer Confluence-Seite, die niemand pflegt."

---

## [20:00–25:00] Tests + Constitution-Compliance (5 Min)

> 1. `tests/architecture/HexagonalArchitectureTest.java` — 5 ArchUnit-Regeln
>    laufen in CI; eine Domain-Klasse die `quickfix.*` importiert lässt
>    den Build scheitern.
> 2. `libs/domain-model/.../OrderStateMachinePropertyTest.java` — jqwik mit
>    1900 random transitions; Invariante "nach FILLED nur TRADE_BUSTED" wird
>    deterministisch geprüft.
> 3. `libs/audit-chain/.../HashChainWriterTest.java` — Property-Test der
>    SHA-256-Kette + ein konkreter Tampering-Test der den gebrochenen
>    Hash erkennt.
> 4. `libs/fix-codec-py/tests/test_roundtrip.py` — 200 zufällige FIX 4.4
>    Roundtrips via Hypothesis (LIVE laufen lassen):
>    ```bash
>    uv run --project libs/fix-codec-py --extra test pytest libs/fix-codec-py/tests/ -q
>    ```

**Hauptbotschaft**: "Verfassungsprinzip VII (`Test-First für Protocol Code`)
ist NICHT-VERHANDELBAR — jeder PR der einen Codec ändert braucht einen
property-test im selben PR. CI erzwingt das."

---

## [25:00–30:00] Operational Excellence + Wrap-Up (5 Min)

> 1. `ops/grafana/dashboards/oms-roundtrip.json` — Grafana-Dashboard für US1.
> 2. `ops/runbooks/oms-recovery-from-drop-copy.md` — "wenn ich um 3 Uhr
>    nachts gerufen werde, was mache ich?"
> 3. `tools/ptp-audit-report/` — Go-Tool für die jährliche RTS-25 FINMA-
>    Audit-Pack-Erzeugung. **Live laufen lassen** (vorbereitete CSV-Datei):
>    ```bash
>    go run ./tools/ptp-audit-report/cmd/ptp-audit-report/ \
>      --input /tmp/ptp-samples.csv --period "2026-Q2" --out /tmp/audit.txt
>    cat /tmp/audit.txt
>    ```
>    Audit-Pack ist signiert (sha256), Verletzungen werden hervorgehoben.
> 4. `docs/decisions/0004-aeron-vs-kafka.md` (oder eine andere ADR) —
>    "warum diese Entscheidung, was wurde abgelehnt?"

**Hauptbotschaft**: "Regulatorik, Operational-Pain, und Architektur-
Entscheidungen sind hier alle in einem Repo, alle versioniert, alle
zugreifbar — und der Audit-Pack ist 5 Sekunden weg auf Kommando."

---

## Letzte Worte

Schließe mit einem ehrlichen Satz:

> "Das ist OSS-Reference. Production an einer echten Tier-1-Bank
> braucht zusätzlich kommerzielle Komponenten — Chronicle FIX,
> Pico Corvil für Wire-Tapping, Geneos, kdb+. Diese sind in den ADRs
> als Drop-Ins dokumentiert, weil ich nicht so tun will, als sei OSS
> genug. Aber die *Architektur* — Hexagonal, Latency-Hierarchie,
> Drop-Copy als Source-of-Truth, Hash-Chained-Audit, RTS-25-PTP — die
> ist hier produktionsfertig modelliert."

---

## Anti-Pattern: Was nicht in den Walkthrough gehört

- Live-Coding (zu viel Risiko, zu wenig Zeit)
- Detail-Diskussion über `quickfix.Session.send()` (zu tief)
- Spring-Boot-Magie (jeder kennt's; verschwendet Zeit)
- Frontend-Aspekte (bei Trading-Engineering-Hiring nebensächlich)

## Wenn der Termin überzieht

Wenn der EM nach 35 Min noch fragt, läuft es gut. Bereithalten:

- ADR-Browser (`docs/decisions/`) — randomly öffnen, eine Frage ergibt sich.
- `tests/chaos/oms-outage-with-dropcopy.yaml` — "wir spielen den 50-Min-
  OMS-Outage durch und prüfen mechanisch, dass die Hash-Chain hält."
- `docs/interview/hard-questions.md` — vorbereiteten harten Fragen.
