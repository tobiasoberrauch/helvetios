# Runbook: Kill-Switch Drill

**Sev**: 1 wenn echte Trip nötig; 3 als geplante Drill.

## Geplanter Drill (quartalsweise)

1. **Vorbereitung**: Test-Trader `alice.drill` auf isoliertem Test-Pfad.
2. **Trip durchführen**:
   ```bash
   curl -X POST http://entitlements:8082/api/v1/killswitch/TRADER/alice.drill/trip \
     -H 'Content-Type: application/json' \
     -d '{"reason":"quarterly drill","tripperUserId":"ops.alice"}'
   ```
3. **Verifikation**:
   - Audit-Chain hat einen `killswitch.trip` Event (siehe `audit.command.v1`).
   - Inbound-FIX-Acceptor lehnt neue Orders mit `KILL_SWITCH_TRIPPED`
     ab (FIX `BusinessMessageReject(35=j)`).
   - Pretrade-risk-gateway-Log zeigt rejected-Orders.
4. **Reset (4-Eyes)** — anderer User als Tripper:
   ```bash
   curl -X POST http://entitlements:8082/api/v1/killswitch/TRADER/alice.drill/reset \
     -d '{"resetterUserId":"ops.bob"}'
   ```

## Notfall-Trip (echter Vorfall)

Sofortige Eskalation an Trading-Floor und Compliance:

```bash
# Alle Orders eines Clients sofort canceln
curl -X POST http://entitlements:8082/api/v1/killswitch/CLIENT/ACME-CAPITAL/trip \
  -d '{"reason":"runaway algo — manual circuit breaker","tripperUserId":"oncall"}'
```

Der Audit-Eintrag MUSS detailliert sein (welcher Algo, welche Symptome,
wer hat entschieden) — diese Logs werden in der nachgelagerten Forensik
und potenziell vor FINMA verwendet.
