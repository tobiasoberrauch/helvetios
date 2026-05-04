# Runbook: Bloomberg BLPAPI UUID / Identity Recovery

**Sev**: 2.

## Symptome

- BLPAPI Subscriptions error `NotEntitledForOperation`.
- EMSX-API liefert `Identity not authorized`.
- Trader-UI zeigt `subscription DENIED` für vorher-funktionsfähige Märkte.

## Ursachen

1. UUID/SerialNumber/AuthID drift zwischen Bloomberg-Member-Portal und
   `entitlements-service` Cache.
2. EMRS-Sync ist > 24h alt.
3. AppID-Konflikt (zwei Konsumenten teilen sich eine).

## Sofortmaßnahmen

```bash
# Cache zurücksetzen
curl -X POST http://entitlements:8082/api/v1/admin/bloomberg/identity-cache/refresh

# Prüfen welche Identity in welcher Region aktiv ist
curl -s http://entitlements:8082/api/v1/admin/bloomberg/identities \
  | jq '.[] | select(.appId=="swisstms-trading-zh-01")'
```

## Recovery

Bei drift: Bloomberg-Member-Portal-Eintrag und unsere Config syncen.
Constitution V — AppID pro Konsument einzigartig, niemals teilen.
