# Runbook: OMS Recovery from Drop-Copy

**Sev**: 1 (kompletter OMS-Outage) bis 3 (kleinere Mismatches).
**Verfassungsbezug**: Principles V (Drop-Copy ist Source-of-Truth) + VI (Append-Only Audit).

## Symptome

| Pager / Alert | Wahrscheinliche Ursache |
|---|---|
| `ReconciliationMismatchPersists` für > 5 min | OMS down ODER OMS state hat einen Fill nicht aufgenommen |
| `DropCopyStreamGap` | Drop-Copy-FIX-Session unterbrochen |
| `ReconcilerLag` > 10 min | Reconciler-Service down oder Kafka-Backpressure |
| OMS REST `/actuator/health` returned `DOWN` | OMS down |

## Sofortmaßnahmen (60 Sekunden)

1. **Trading pausieren?** Nur wenn Drop-Copy-Stream ebenfalls weg ist (keine Source-of-Truth verfügbar). Dann Kill-Switch für betroffene Venues per
   ```bash
   curl -X POST -H "Authorization: Bearer ${TOKEN}" \
     https://entitlements.zh.swisstms.local/api/v1/killswitch/CLIENT/all/trip \
     -d '{"reason":"OMS+drop-copy stream lost — manual investigation"}'
   ```
2. **OMS-Status prüfen**:
   ```bash
   kubectl -n tms-prod-shadow-zh logs deploy/oms-oms-service --tail=200
   kubectl -n tms-prod-shadow-zh describe pod -l app.kubernetes.io/name=oms-service
   ```
3. **Drop-Copy-Stream prüfen**:
   ```bash
   kafka-consumer-groups.sh --bootstrap-server kafka:9092 --describe --group swisstms-reconciler
   kafka-console-consumer.sh --bootstrap-server kafka:9092 --topic warm.dropcopy.six.v1 --max-messages 10
   ```

## Recovery-Prozedur

### 1. OMS hochfahren

```bash
kubectl -n tms-prod-shadow-zh rollout restart deploy/oms-oms-service
kubectl -n tms-prod-shadow-zh rollout status deploy/oms-oms-service --timeout=300s
```

Beim Start spielt `DropCopyRecoveryJob` automatisch alle Fills aus
`cold.exec.fill.v1` ab dem letzten OMS-Event-Timestamp ein. Constitution V:
Drop-Copy gewinnt — der OMS-State wird ggf. überschrieben.

### 2. Reconciliation-Mismatches abarbeiten

Mismatches auf `warm.recon.mismatch.v1` werden nicht automatisch
aufgelöst. Manuelle Triage:

```bash
kafka-console-consumer.sh --bootstrap-server kafka:9092 \
  --topic warm.recon.mismatch.v1 --from-beginning --max-messages 50
```

Mögliche Mismatches:

- **`DROPCOPY_ONLY`** — OMS hat den Fill nicht. Recovery-Job hat ihn
  bereits via Audit-Chain als `recon.amendment` registriert; OMS-State
  wird beim nächsten Restart nachgezogen.
- **`OMS_ONLY`** — OMS-Phantom-Fill. Untersuchen: ist es ein DK-Trade
  (DontKnowTrade)? Wenn ja: Drop-Copy hat Recht, OMS-Eintrag wird
  Storniert (siehe ADR-0008).
- **`FIELD_MISMATCH`** — z.B. unterschiedliche Quantity/Price.
  Drop-Copy gewinnt (Constitution V) — OMS-Eintrag wird amendiert,
  Audit-Chain trägt `recon.amendment` ein.

### 3. Audit-Chain verifizieren

Tägliche Verifikation läuft per `apps/audit-service/HashChainVerifier`.
Manueller Check:

```sql
SELECT order_id, count(*) as events,
  bool_and(
    seq=1 OR prev_hash=lag(hash) OVER (PARTITION BY order_id ORDER BY seq)
  ) as chain_ok
FROM order_event
WHERE biz_time > NOW() - INTERVAL '1 hour'
GROUP BY order_id
HAVING bool_and(seq=1 OR prev_hash=lag(hash) OVER (PARTITION BY order_id ORDER BY seq)) IS DISTINCT FROM TRUE;
```

Jede Zeile = potenziell gebrochene Hash-Chain. Sofort eskalieren an die
Architecture Review Board (Constitution VI ist NICHT-VERHANDELBAR).

### 4. Trading wieder freigeben

```bash
curl -X POST -H "Authorization: Bearer ${TOKEN_DIFFERENT_USER}" \
  https://entitlements.zh.swisstms.local/api/v1/killswitch/CLIENT/all/reset
```

Die 4-Eyes-Regel verlangt einen anderen Operator als den Tripper.

## Post-Mortem-Pflichtteile

- Nach jeder Wiederherstellung mit > 100 Mismatches: Post-Mortem-ADR
  unter `docs/decisions/0xxx-pm-<datum>-oms-recovery.md`.
- Audit-Chain-Bruch: sofortiger Sev-1 Incident, Eskalation an die Bank
  Compliance + FINMA-Meldung gemäß FinfraG Art. 39.

## Verwandte Dokumente

- `docs/decisions/0008-drop-copy-source-of-truth.md` — Architektur-ADR
- `apps/reconciler-service/src/main/java/.../ReconcilerTopology.java` — Code
- `tests/chaos/oms-outage-with-dropcopy.yaml` — Chaos-Test der diese Prozedur durchspielt
