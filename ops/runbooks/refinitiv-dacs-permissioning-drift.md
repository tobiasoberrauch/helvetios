# Runbook: Refinitiv DACS Permissioning Drift

**Sev**: 2.

## Symptome

- Stream `cold.surveillance.alert.v1` zeigt unerwartet niedrige Tick-Counts.
- `OpenDACS.checkSubscription()` returnt `false` für vorher-zugeordnete RICs.
- PE-Codes in `PROD_PERM` (FID 1) ändern sich plötzlich.

## Sofortmaßnahmen

```bash
# DACS Permission List re-sync
curl -X POST http://entitlements:8082/api/v1/admin/refinitiv/dacs/sync

# Prüfen welche PE-Codes für unsere AppId aktuell zugewiesen sind
kubectl exec -n tms-dev deploy/venue-adapter-refinitiv -- \
  curl -s localhost:8104/actuator/metrics/refinitiv.dacs.pe-code-count
```

## Recovery

Bei drift: in Refinitiv Account-Manager prüfen ob die Lizenz noch
gültig ist (PE-Codes können nach Lizenz-Erneuerung kurzzeitig ungültig sein).

Hinweis: AppId muss pro Konsument einzigartig sein — niemals zwischen
OpenDACS und TREP-Consumer teilen.
