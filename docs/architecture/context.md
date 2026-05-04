# C4 Level 1 — System Context

```mermaid
flowchart LR
  Trader([Trader / Portfolio Manager])
  Compliance([Compliance Officer])
  Ops([Market Support / IT-Ops])
  Client([Buy-side client — FIX-as-server])
  FINMA([FINMA / SIX Trade Repository])
  ARM([LSEG TRADEcho ARM])
  EmirTr([DTCC GTR + REGIS-TR])

  subgraph TMS[Swiss Trading & Market Support Platform]
    Core[Reference Mono-Repo]
  end

  SIX[(SIX Swiss Exchange<br/>OTI/STI/QTI/IMI/MDDX/TRI)]
  EUREX[(Eurex T7 + C7 Clearing)]
  BLP[(Bloomberg<br/>BLPAPI / EMSX / DL)]
  RDP[(LSEG Refinitiv<br/>RTSDK / RDP / DACS)]
  TW[(Tradeweb<br/>TradeXpress + AiEX)]
  MA[(MarketAxess<br/>Open Trading + Trax APA)]
  BFX[(BidFX<br/>Pixie / Puffin)]
  CFETS[(CFETS via Bloomberg / Tradeweb)]
  XCLEAR[(SIX x-clear / SECOM)]

  Trader --> Core
  Client --> Core
  Compliance --> Core
  Ops --> Core
  Core --> SIX
  Core --> EUREX
  Core --> BLP
  Core --> RDP
  Core --> TW
  Core --> MA
  Core --> BFX
  Core --> CFETS
  Core --> XCLEAR
  Core -.RTS-22.-> ARM
  Core -.FinfraG Art. 39.-> FINMA
  Core -.EMIR.-> EmirTr
```

The canonical Structurizr DSL source for this and the Container view lives at
`tools/architecture/workspace.dsl`. Run Structurizr Lite locally to render.
