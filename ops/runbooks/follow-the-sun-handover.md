# Runbook: Follow-the-Sun Handover

**Sev**: 3 (regulärer Vorgang). 1 nur bei verlorenen Orders während Handover.
**Verfassungsbezug**: FR-042d / SC-019.

## Schedule

| UTC | Event |
|---|---|
| 06:00 | TY3 → LD4 (Tokyo close, London open) |
| 14:00 | LD4 → NY4 (London close, NY open) |
| 22:00 | NY4 → TY3 (NY close, Tokyo open) |

`apps/region-router/CutoverScheduler` publiziert pro Cutover ein Event auf
`region.handover.cutover.v1`. Audit-Chain-Eintrag: `region.handover.executed`.

## Validierung pro Cutover

Nach jedem Cutover läuft automatisch:

1. **Order-Continuity-Check** (Spring Batch Job in der nächsten Region):
   ```sql
   -- Keine in-flight Order darf im "alten" Region-Tag sein, wenn ihre
   -- letzte Update-Time älter als der Cutover ist.
   SELECT count(*) FROM order_aggregate
   WHERE region = 'TY3'
     AND ord_status IN ('NEW','ACKNOWLEDGED','PARTIALLY_FILLED')
     AND last_updated_at < (SELECT MAX(biz_time) FROM region_handover
                            WHERE from_region='TY3' AND to_region='LD4');
   ```
   Erwartung: `0` Zeilen. Sonst → Sev-2 Alert.

2. **Audit-Chain-Cross-Region-Verifikation** — der Chain in der neuen
   Region MUSS lückenlos beim letzten `region.handover.received` Event
   anschließen.

## Wenn Handover fehlschlägt

Symptome:
- `RegionHandoverIncomplete` Alert.
- Order-Continuity-Check meldet > 0 in-flight Orders im alten Region-Tag.

Pfad:
1. Cutover manuell stoppen (Spring profile activate `safe-mode`).
2. Liste der hängenden Orders extrahieren (aus dem SQL oben).
3. Pro Order entscheiden: weiterführen in alter Region (manueller Override
   in `region-router` config) oder cancel + neu in der neuen Region
   einsteuern.
4. Post-mortem ADR.
