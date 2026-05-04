# Adding a New Venue

Constitution Principle I — adding a venue is **additive**: only the new
adapter module is touched. The domain core, OMS, EMS, reconciler,
reporting and surveillance services do not need code changes.

## 90-Sekunden-Antwort im Interview

> "Wenn ich morgen Cboe Europe anbinden müsste:
> ich rufe `make new-venue NAME=cboe`, das scaffoldet
> `apps/venue-adapter-cboe/` mit einem `CboeVenueAdapter` der bereits
> `VenueGatewayPort` implementiert. Ich fülle die TODOs aus, schreibe
> einen Conformance-Test gegen FIXimulator (oder den vendor-spezifischen
> Mock), wire einen `warm.dropcopy.cboe.v1`-Producer ein, und das
> Spring-DI im OMS pickt den neuen Adapter automatisch auf. Domain,
> OMS, EMS, Reconciler, Reporting, Surveillance werden nicht angefasst —
> der `HexagonalArchitectureTest` in CI lässt das gar nicht zu."

## Workflow im Detail

```bash
# 1. Scaffold
make new-venue NAME=cboe

# Erzeugt:
#   apps/venue-adapter-cboe/
#     ├── build.gradle.kts
#     ├── src/main/java/ch/swisstms/venue/cboe/CboeVenueAdapter.java
#     ├── src/main/resources/application.yml
#     └── src/test/java/.../CboeBasicLifecycleTest.java
#   docs/decisions/0xxx-venue-adapter-cboe.md
#   settings.gradle.kts wird ergänzt
```

```bash
# 2. Implementieren
$EDITOR apps/venue-adapter-cboe/src/main/java/ch/swisstms/venue/cboe/CboeVenueAdapter.java

# Ersetze die TODOs:
#   - VENUE_MIC = "BATY" (oder die korrekte Cboe Europe MIC)
#   - submitOrder/cancelOrder/replaceOrder mit dem Cboe-FIX-Dialekt
#   - executions() Hot-Publisher mit echter ExecutionReport-Map.
```

```bash
# 3. Conformance test
$EDITOR apps/venue-adapter-cboe/src/test/java/ch/swisstms/venue/cboe/CboeBasicLifecycleTest.java
# Mirror tests/conformance/six-sti/SixStiBasicLifecycleTest.java —
# NEW → PARTIAL_FILL → FILL chain via FIXimulator (oder Cboe-Mock).
```

```bash
# 4. Drop-copy
$EDITOR apps/venue-adapter-cboe/src/main/java/ch/swisstms/venue/cboe/CboeDropCopyProducer.java
# Spring-Component die den execution stream auf
# warm.dropcopy.cboe.v1 publishet (Constitution V).
```

```bash
# 5. Tilt-Extension
cat > tools/tilt/extensions/venue_adapter_cboe.star <<'STAR'
custom_build('ghcr.io/.../swisstms-venue-adapter-cboe',
    './gradlew :apps:venue-adapter-cboe:bootBuildImage --imageName=$EXPECTED_REF',
    deps=['apps/venue-adapter-cboe/src/main', ...])
k8s_yaml(helm('apps/venue-adapter-cboe/helm', name='venue-cboe'))
STAR
```

```bash
# 6. Build & test
./gradlew :apps:venue-adapter-cboe:test
./gradlew :tests:architecture:test  # ArchUnit verifies hexagonal discipline
```

## Was sich NICHT ändert

CI prüft mechanisch (HexagonalArchitectureTest), dass keiner der folgenden
Pfade durch das Hinzufügen eines Adapters berührt wurde:

- `libs/domain-model/`
- `apps/oms-service/`
- `apps/ems-service/`
- `apps/reconciler-service/`
- `apps/reporting-service/`
- `apps/surveillance-service/`
- `libs/audit-chain/`
- `libs/time-sync/`

Wenn der PR diese Pfade verändert, ist es **kein** sauberes Adapter-Onboarding
mehr — entweder ist die Spec falsch (dann ADR), oder der Code hat einen
Hack eingeführt, der den Hexagonal-Vertrag bricht (dann zurück ans
Reissbrett).

## CODEOWNERS

`.github/CODEOWNERS` enthält bereits ein generisches Pattern:

```
/apps/venue-adapter-*/                @swisstms/adapters-<asset-class>
```

Pro neuem Adapter eine zusätzliche Zeile mit dem konkreten Owner-Team
hinzufügen, z.B.:

```
/apps/venue-adapter-cboe/             @swisstms/adapters-equities
```

## Pact

Der Pact-Konsumenten-Test in `apps/oms-service/src/test/.../`
(`OmsVenueGatewayPortPact`) wird beim Build automatisch gegen jeden
neuen Adapter verifiziert — keine Änderung an der Pact-Datei nötig
(der Vertrag ist `VenueGatewayPort`, nicht eine adapter-spezifische
Variante).
