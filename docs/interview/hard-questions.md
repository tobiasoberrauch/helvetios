# Wahrscheinlich harte Interview-Fragen

Antwort-Format: **Frage** → **Kurz-Antwort (1 Satz)** → **Repo-Beleg** → **Nuance**.

---

## 1. "Warum nicht alles in Rust?"

**Kurz**: Tier-1-Banken haben keine Rust-Hot-Path-Production publik dokumentiert; Talent-Pool und Operational-Readiness sprechen für Java + Python + TypeScript.

**Repo-Beleg**: `docs/decisions/0011-rust-not-in-v1.md`. `Cargo.toml` ist ein leerer Workspace-Stub — Rust ist optional für Crypto-Adapter.

**Nuance**: Aeron's Latenz-Beweis ist auf der JVM. Ein Rust-Hot-Path würde zwar GC-Pausen vermeiden, aber kein Tier-1-Beispiel zeigt, dass das den Wechsel rechtfertigt. Polyglott-Ehrlichkeit > Tech-Religion.

---

## 2. "Warum QuickFIX/J statt OnixS oder Chronicle FIX?"

**Kurz**: Kostenneutralität für die Reference-Impl + dokumentierter Migrationspfad zu OnixS bei Production-Hardening.

**Repo-Beleg**: `docs/decisions/0005-quickfixj-vs-onixs.md`. **Artio** (von Real Logic, OSS, Aeron-basiert) für den Hot-Path zeigt zwei-Tier-Strategie.

**Nuance**: QuickFIX/J 2.3.2 schafft 10–30k msg/s/Session. Für 99% der Vendor-Sessions reicht das. Für sell-side inbound (Phase 13) wird Artio benötigt — und der ist OSS.

---

## 3. "Wie würdest du eine 50-Min-Outage handhaben, in der der OMS down war und der Drop-Copy 12.000 Fills aufgesammelt hat?"

**Kurz**: Drop-Copy ist Source-of-Truth (Constitution V); OMS rekonstruiert State per Replay aus `cold.exec.fill.v1` mit `recon.amendment` Audit-Chain-Eintrag pro nachgezogenen Fill.

**Repo-Beleg**: `ops/runbooks/oms-recovery-from-drop-copy.md` + `apps/oms-service/.../recovery/DropCopyRecoveryJob.java` + `tests/chaos/oms-outage-with-dropcopy.yaml`.

**Nuance**: Der Reconciler-Service (`apps/reconciler-service/`) macht das automatisch. Die Hash-Chain im `order_event` table wird von einem PL/pgSQL-Trigger erzwungen — Tampering ist auf DB-Level abgewehrt.

---

## 4. "Wie evidenzierst du RTS-25 Compliance gegenüber FINMA?"

**Kurz**: `tools/ptp-audit-report/` produziert ein signiertes Report-File mit Median + p99 + Max-Divergenz pro Server, traceable to UTC via Meinberg-GM-Logs.

**Repo-Beleg**: `tools/ptp-audit-report/cmd/ptp-audit-report/main.go` — live laufbar gegen jede CSV von ptp4l/phc2sys-Logs. Output enthält SHA-256-Hash; cosign-Signatur auf Wunsch.

**Nuance**: RTS-25 verlangt ≤ 100µs für Trading-Server. Hardware: Meinberg LANTIME M3000 + Solarflare/Mellanox NICs mit HW-Timestamping. Bestätigung in `infra/ansible/playbooks/ptp-grandmaster.yml` (Phase 11/14).

---

## 5. "Was passiert, wenn die Eurex-AMQP-CA morgen rotiert wird und wir es verpassen?"

**Kurz**: cert-manager + OpenBao PKI Auto-Renewal; AlertManager-Rule `EurexTruststoreCertExpiryWarning` feuert 30 Tage vor Ablauf; Notfall-Runbook für manuelle Rotation.

**Repo-Beleg**: `ops/prometheus/alerts/eurex-cert-expiry-30d.yml` + `ops/runbooks/eurex-amqp-cert-rotation.md` + `apps/clearing-adapter-eurex/.../CertRotationAuditor.java`.

**Nuance**: Eurex rotiert jährlich im September. Auto-Renewal ist im prod-shadow ENV, im Notfall-Pfad wechselt man via `keytool` und `kubectl rollout restart` (siehe Runbook).

---

## 6. "Warum Event-Sourcing für OMS, mit allen Trade-Offs (Schema-Migration, Replay-Komplexität)?"

**Kurz**: Deterministische Replay (Chaos-Tests, RTS-24 Compliance, Audit-Replay) — Schema-Migration via Versioned-Events + Upcasters.

**Repo-Beleg**: `apps/oms-service/.../infra/OrderEventEntity.java` + `apps/oms-service/src/main/resources/db/migration/V3__order_event.sql` mit dem `enforce_event_chain` Trigger.

**Nuance**: Wir machen kein "pure event sourcing" (Aggregate-Hydration aus Events). Wir machen Outbox-pattern + append-only event log + state in `order_aggregate`. Das gibt uns Replay ohne den vollen ES-Komplexitäts-Tax.

---

## 7. "Wie skaliert Aeron Cluster, wenn ein Knoten ausfällt?"

**Kurz**: Raft-Replikation (5-Knoten-Cluster, Quorum=3) per Region. Leader-Kill → automatische Re-Election in <100ms.

**Repo-Beleg**: `tests/chaos/aeron-cluster-leader-kill.yaml` (Phase 16). Phase 14 multi-region: **per-region** Aeron Cluster, weil Raft nicht über WAN funktioniert.

**Nuance**: Cross-region geht via Kafka MirrorMaker 2 + Aurora Global DB für state, nicht via Aeron. Latency-Budget für Hot-Path verlangt das.

---

## 8. "Wie würdest du Bloomberg-Entitlements zentral durchsetzen?"

**Kurz**: `apps/entitlements-service/` mit BLPAPI-EMRS-Sync, OpenDACS-Bridge für Refinitiv, gemeinsame `EntitlementPort`-API; Pre-Trade-Risk-Gateway prüft synchron.

**Repo-Beleg**: `libs/domain-model/.../ports/EntitlementPort.java` + `apps/entitlements-service/.../InMemoryEntitlementService.java`.

**Nuance**: Bloomberg Identity-Cache hat 24h TTL, EMRS-Sync nightly. Ein Entitlement-Source-Outage löst FAIL-CLOSED aus (FR-021) — bessere Variante als FAIL-OPEN, weil Vendor-Vertragsbruch teurer ist als ein paar Minuten ohne neue Subscriptions.

---

## 9. "Sell-side inbound bei 10M Orders/Tag — wie verteilst du das?"

**Kurz**: 200 inbound FIX-Sessions per Region, Artio-basiert. Pre-Trade-Risk-Gateway ist single-writer auf Disruptor mit p99 < 50µs (SC-017). Aeron IPC zwischen Acceptor und Risk-Gateway.

**Repo-Beleg**: `apps/inbound-fix-acceptor/` + `apps/pretrade-risk-gateway/` (Phase 13). `contracts/sbe/orders.xml` für die Aeron-IPC-Messages.

**Nuance**: Acceptor und Risk-Gateway laufen auf dem gleichen physischen Host (sonst geht der 50µs-Budget kaputt). Risk-Profile sind off-heap in Agrona-Map; updates via Kafka-Topic `warm.entitlements.limit-update.v1`.

---

## 10. "Was ist der eine Punkt, an dem dieses Repo *nicht* tier-1-glaubhaft ist?"

**Kurz**: kdb+/q-Lücke. Das gesamte Tick-Storage läuft auf QuestDB+ClickHouse, nicht auf kdb+ wie bei Morgan Stanley / Goldman / RBC / UBS.

**Repo-Beleg**: `docs/decisions/0004-aeron-vs-kafka.md` (Tick-Storage-Drop-In dokumentiert).

**Nuance**: kdb+ ist commercial, dekadenlang bewährt für Nanosekunden-Queries. Für eine OSS-Reference ist QuestDB+ClickHouse vertretbar; in echter Tier-1-Production würde der erste Drop-In-Move sein, kdb+ einzubinden.

---

## Bonus: "Was würdest du ändern, wenn du nochmal von vorne anfangen würdest?"

**Ehrliche Antwort**:
1. **Earlier ArchUnit** — die hexagonale Disziplin hätte ich von Tag 1 mechanisch erzwingen sollen, nicht erst in Phase 5.
2. **Schema-Registry-First** — Apicurio von Tag 1, nicht erst nach den Stub-XSDs.
3. **Multi-Region NICHT in der ersten Architektur** — das hat den Plan deutlich verkompliziert. Ich würde mit Single-Region starten und multi-region als Phase 2 deklarieren.
