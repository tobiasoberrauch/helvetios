# ISO 20022 Templates

Verwendet für SIX SECOM Settlement-Instructions.

| Datei | ISO 20022 Message | Verwendung |
|---|---|---|
| `sese.023.001.xx.xsd` | SecuritiesSettlementTransactionInstruction | SECOM Outbound — Bank → x-clear |
| `sese.025.001.xx.xsd` | SecuritiesSettlementTransactionConfirmation | SECOM Inbound — x-clear → Bank |

Volle XSDs werden via `make scaffold` aus `iso20022.org/payments_messages.page`
oder dem SIX-Member-Portal bezogen. Die Stubs in diesem Ordner reichen
aus, damit die JAXB-Codegen-Pipeline (`tools/codegen/jaxb.gradle.kts`)
konfiguriert werden kann.
