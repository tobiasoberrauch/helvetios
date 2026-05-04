# Outbound FIX Initiator Configuration

**Service**: each `apps/venue-adapter-*` that uses FIX (SIX STI, Eurex T7 FIX gateway, Tradeweb, MarketAxess, Bloomberg EMSX fallback, Trax APA).

QuickFIX/J initiators are configured per venue in `apps/venue-adapter-<venue>/src/main/resources/quickfix/`. The `JdbcStoreFactory` writes session state to Postgres so that sequence numbers survive process restarts.

## QuickFIX `.cfg` example (SIX STI)

```ini
[default]
ConnectionType=initiator
StartTime=06:00:00 UTC
EndTime=22:30:00 UTC
HeartBtInt=30
ReconnectInterval=5
FileLogPath=/var/log/quickfix/six-sti
JdbcDriver=org.postgresql.Driver
JdbcURL=jdbc:postgresql://oms-db:5432/quickfix
JdbcUser=quickfix
SocketUseSSL=Y
SSLProtocol=TLSv1.3
SSLKeyStore=/etc/swisstms/keystores/six-sti.p12
SSLKeyStorePassword=#{vault:secret/data/six-sti/keystore-password}

[session]
BeginString=FIX.4.4
SenderCompID=SWISSTMS
TargetCompID=SIX-STI
SocketConnectHost=fix.six-group.com
SocketConnectPort=8543
DataDictionary=contracts/fix/venues/SIX_STI_FIX44.xml
ResetOnLogon=N
ResetOnLogout=N
ResetOnDisconnect=N
RefreshOnLogon=Y
SocketUseSSL=Y
```

## Per-venue settings (key differences)

| Venue | BeginString | DataDictionary | Custom session opts |
|---|---|---|---|
| SIX STI | FIX.4.4 | `SIX_STI_FIX44.xml` | Daily ResetSeqNumFlag at session-start window |
| Eurex T7 FIX gateway | FIX.4.2 / FIX.4.4 | `EUREX_T7_FIX42.xml` | `PartyIDExecutingTrader (20036)` populated from entitlements |
| Tradeweb TradeXpress | FIXT.1.1 / FIX 5.0 SP2 | `TRADEWEB_TradeXpress.xml` (v101.34) | RFQ flow `R/S/D/8` |
| MarketAxess Open Trading | FIXT.1.1 / FIX 5.0 SP2 | `MARKETAXESS_OPEN_TRADING.xml` | EP228 + Trax custom tags |
| Trax APA | FIXT.1.1 / FIX 5.0 SP2 | `TRAX_APA_FIX50SP2.xml` | Daily session-reset 23:00–23:05 GMT; CSV-SFTP fallback ≥ 3GB |
| Bloomberg EMSX (FIX fallback) | FIX.4.4 | `BLOOMBERG_EMSX_FIX44.xml` | EMRS entitlement sync; not used in production (BLPAPI is the primary) |

## Sequence-number persistence

Custom `JdbcStoreFactory` writes to Postgres table:

```sql
CREATE TABLE fix_session_state (
  sender_comp_id      TEXT NOT NULL,
  target_comp_id      TEXT NOT NULL,
  session_qualifier   TEXT NOT NULL DEFAULT '',
  next_sender_seq     BIGINT NOT NULL,
  next_target_seq     BIGINT NOT NULL,
  last_logon          TIMESTAMPTZ,
  PRIMARY KEY (sender_comp_id, target_comp_id, session_qualifier)
);
```

Row-level locking on `SELECT ... FOR UPDATE` prevents concurrent updates from competing instances.

## Gap recovery pattern

On `ResendRequest(35=2)`, the application-layer mapper distinguishes:

- **Application messages** (35=D, 35=8, 35=F, 35=G, 35=9, 35=AE, 35=AR, 35=R, 35=S): real replay from the session log with `PossDupFlag(43)=Y`.
- **Admin messages** (35=A, 35=0, 35=1, 35=2, 35=4, 35=5): respond with `SequenceReset-GapFill (35=4, GapFillFlag(123)=Y)`.

Chaos tests in `tests/chaos/fix-session-drop.yaml` verify automatic recovery.
