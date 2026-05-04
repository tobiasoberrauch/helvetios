# FIXimulator Mock

Sell-side FIX-Counterparty für Initiator-Tests. Wird per Docker-Container
gestartet (`compose.dev.yaml` Service `fiximulator`). Eine Acceptor-Session
pro Venue-Adapter (Ports 9876–9890).

## Phase 2 (T053) — Container

Phase 2 liefert nur den Container-Stub (FIXimulator selbst ist kein
publishables Maven-Artifact und muss aus `https://github.com/fiximulator/`
kompiliert werden). Bis dahin nutzen `tests/conformance/six-sti/` einen
selbstgebauten Mini-Acceptor in `mocks/six-mts-stub/` (Phase 3).

## Konfiguration

```ini
# mocks/fiximulator/cfg/acceptor.cfg
[default]
ConnectionType=acceptor
StartTime=00:00:00 UTC
EndTime=23:59:59 UTC

[session]
BeginString=FIX.4.4
SenderCompID=SIX-STI
TargetCompID=SWISSTMS
SocketAcceptPort=9876
DataDictionary=contracts/fix/venues/SIX_STI_FIX44.xml
```
