# Runbook: SIX STI Sequence Number Resync

**Sev**: 2.

## Symptome

- FIX `Reject(35=3)` mit `RefSeqNum` größer als erwartet.
- FIX `ResendRequest(35=2)` flutet die Session.
- Grafana `fix-session-health` zeigt steigende `gap_fills`-Metric.

## Sofortmaßnahmen

```bash
# 1. Session-Status checken
kubectl exec -n tms-dev deploy/venue-adapter-six -- \
  curl -s localhost:8101/actuator/metrics/fix.session.gap_fills | jq

# 2. Sequence-Number-State in Postgres
psql $PG_URL -c "SELECT * FROM fix_session_state WHERE sender_comp_id='SWISSTMS' AND target_comp_id='SIX-STI';"

# 3. Wenn Session in einem inkonsistenten State: forcieren eines
#    geordneten Logout + Logon mit ResetSeqNumFlag(141)=Y. Daily-Reset
#    Window ist 06:00 UTC.
```

## Wiederherstellungs-Pfad

1. **In-Session Resync** (default): bei einer ResendRequest-Schleife
   reagiert der QuickFIX-Engine mit SequenceReset-GapFill (35=4).
2. **Session-Reset** (eskaliert): manueller Reset über Daily-Reset-Window
   (06:00 UTC) mit `ResetSeqNumFlag(141)=Y` im Logon.
3. **DB-Recovery**: bei Postgres-Schäden (z.B. nach Ausfall) — aus
   einem PG-PITR-Snapshot wiederherstellen, sequence-numbers manuell
   syncen mit der bekannt letzten Position aus den FIX-Logs in
   OpenSearch.

## Verwandt

- `tests/chaos/fix-session-drop.yaml` simuliert dies in CI.
- `libs/fix-codec/.../session/SessionLifecycle.java` — Code.
