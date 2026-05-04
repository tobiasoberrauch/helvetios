# FpML 5.12 XSDs

Volle FpML 5.12 XSDs aus `fpml.org/spec/fpml-5-12-7-rec-1` werden über
`make scaffold` heruntergeladen. Stubs hier reichen für die
`tests/property/java/FpmlInterestRateStreamTest.java`-Konfiguration.

## Verwendung

OTC-IRS-Konfirmationen via Eurex Clearing OTC. JAXB-Codegen über
`tools/codegen/jaxb.gradle.kts` produziert Java-Klassen unter
`libs/fpml-codec/src/generated/`.
