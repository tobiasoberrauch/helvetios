# Venue-Spezifische FIX-Dictionaries

Jeder Venue-Adapter verwendet sein eigenes FIX-Dictionary. Die Dialekte
sind Subsets oder Erweiterungen der FIX-Standards (4.4 / 5.0 SP2). Stubs
in diesem Ordner enthalten die für Conformance-Tests benötigten Messages;
volle Dialekte werden durch `make scaffold` aus den Vendor-Specs vendored
oder müssen manuell aus den Member-Portalen heruntergeladen werden.

| Venue | Datei | Basis | Quelle |
|---|---|---|---|
| SIX STI | `SIX_STI_FIX44.xml` | FIX 4.4 | SIX Swiss Exchange Member Section |
| Eurex T7 FIX-Gateway | `EUREX_T7_FIX42.xml` | FIX 4.2/4.4 | Eurex Member Section |
| Tradeweb TradeXpress | `TRADEWEB_TradeXpress.xml` | FIXT.1.1 / FIX 5.0 SP2 | OnixS-veröffentlichte Dialekt-Spec v101.34 |
| MarketAxess Open Trading | `MARKETAXESS_OPEN_TRADING.xml` | FIXT.1.1 / FIX 5.0 SP2 | MarketAxess Member Section |
| MarketAxess Trax APA | `TRAX_APA_FIX50SP2.xml` | FIXT.1.1 / FIX 5.0 SP2 + EP228 | MarketAxess Trax Spec |
| Bloomberg EMSX (Fallback) | `BLOOMBERG_EMSX_FIX44.xml` | FIX 4.4 | Bloomberg EMSX-API-Doku |

## Custom-Tag-Registry

Custom Tags pro Venue müssen in `contracts/fix-sessions/dictionaries.md`
eingetragen werden. Der Test
`tests/property/java/fix-tag-registry-no-collision-test.java` prüft
automatisch auf doppelte Belegung über alle Dialekte hinweg
(Verfassungsprinzip III).
