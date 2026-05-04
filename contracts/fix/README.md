# FIX Data Dictionaries

Die FIX-Standard-Dictionaries (`FIX44.xml`, `FIX50SP2.xml`, `FIXT11.xml`) und
die venue-spezifischen Dialekte unter `venues/` sind groß (~6000 Zeilen pro
Datei) und werden bewusst nicht inline gepflegt, sondern aus den
QuickFIX/J-Distribution-JARs entpackt.

## Bezugsquellen

| Datei | Quelle |
|---|---|
| `FIX44.xml`        | `org.quickfixj:quickfixj-messages-fix44:2.3.2` (jar `META-INF/`) |
| `FIX50SP2.xml`     | `org.quickfixj:quickfixj-messages-fix50sp2:2.3.2` |
| `FIXT11.xml`       | `org.quickfixj:quickfixj-messages-fixt11:2.3.2` |
| Venue-Dialekte     | siehe `venues/README.md` |

## Bootstrap

```bash
make scaffold
```

Das `scaffold`-Target lädt die JARs aus Maven Central und entpackt die
Dictionaries hierhin. Bis dahin existieren Stubs, die ausreichen, damit
die Property-Tests in `tests/property/java/` (jqwik) konfiguriert werden
können — die echten Roundtrip-Tests werden gegen die vollständigen
Dictionaries gefahren, sobald `make scaffold` lief.

## Constitution-Bezug

Verfassungsprinzip III (Schemas-as-Versioned-Contracts, NICHT-VERHANDELBAR)
verlangt, dass jede Schema-Änderung im selben PR von einem Contract-Test
begleitet wird. Die FIX-Tag-Registry-Kollisions-Prüfung
(`tests/property/java/fix-tag-registry-no-collision-test.java`) prüft auf
doppelte Custom-Tag-Belegung über alle Dialekte hinweg.
