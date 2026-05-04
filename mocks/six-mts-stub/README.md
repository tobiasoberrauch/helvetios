# SIX MTS-Style Stub

Phase 3 (US1) verwendet keine separate Container-Komponente, sondern den
in-process Mock im `apps/venue-adapter-six/SixStiAdapter` (Property
`swisstms.six.simulate-fills=true`). Sobald in Phase 14 die echte
QuickFIX/J-Initiator-Konfiguration scharfgeschaltet wird, ersetzt dieser
Container den Endpunkt `fix.six-group.com:8543`:

- FIX 4.4 Acceptor auf Port 9876
- OUCH+SoupBinTCP-Acceptor auf Port 19011 (Phase 15)
- ITCH/MoldUDP64-Multicast auf 224.10.0.10:30001 (Phase 15)

## Build (Phase 14+)

```bash
docker build -t swisstms/six-mts-stub:0.1.0 mocks/six-mts-stub/
docker run --rm -p 9876:9876 swisstms/six-mts-stub:0.1.0
```

## Konfiguration

Verzeichnis-Layout (Phase 14):

```
mocks/six-mts-stub/
├── Dockerfile
├── cfg/acceptor.cfg            # QuickFIX/J Acceptor mit SIX_STI_FIX44.xml
├── src/main/java/.../          # Filler-Logik (NEW → PARTIAL_FILL → FILL)
└── README.md
```
