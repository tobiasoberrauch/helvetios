# FIXML 5.0 SP2 XSDs (Eurex C7)

Vollständige FIXML 5.0 SP2 XSDs werden über `make scaffold` aus der
FIX-Trading-Community-Distribution oder direkt aus der Eurex-C7-Member-
Section bezogen (Volumes 1–8 der Eurex-Spec).

Die hier vorliegenden Stub-XSDs reichen aus, damit
`tools/codegen/jaxb.gradle.kts` die Codegen-Pipeline konfigurieren kann
und `tests/property/java/EurexFixmlMessageRoundtripTest.java` die
Roundtrip-Eigenschaft prüft.

## Verzeichnis (nach vollständigem Vendoring)

| Volume | Inhalt |
|---|---|
| 1 | Trade-Capture-Reports |
| 2 | Position-Maintenance |
| 3 | Public-Broadcasts |
| 4 | Margin-Calls |
| 5 | Settlement-Instructions |
| 6 | OTC-IRS (FpML embedded) |
| 7 | Reference-Data |
| 8 | Reports / Statements |
