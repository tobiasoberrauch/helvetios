# SBE Schema — Orders (internal hot-path)

**File**: `contracts/sbe/orders.xml` (canonical), generated codecs in `libs/sbe-codec/`.
**Schema id**: 1
**Used by**: `apps/inbound-fix-acceptor/` ↔ `apps/pretrade-risk-gateway/` ↔ `apps/ems-service/` ↔ `apps/venue-adapter-six/` (hot path) and `apps/venue-adapter-eurex/` (T7 ETI).

## Templates

```xml
<sbe:messageSchema package="ch.swisstms.sbe.orders" id="1" version="1" semanticVersion="1.0.0" byteOrder="littleEndian">
  <types>
    <composite name="messageHeader">
      <type name="blockLength" primitiveType="uint16"/>
      <type name="templateId" primitiveType="uint16"/>
      <type name="schemaId"   primitiveType="uint16"/>
      <type name="version"    primitiveType="uint16"/>
    </composite>
    <enum name="Side" encodingType="uint8">
      <validValue name="BUY">1</validValue>
      <validValue name="SELL">2</validValue>
      <validValue name="SELL_SHORT">3</validValue>
    </enum>
    <enum name="OrdType" encodingType="uint8">
      <validValue name="MARKET">1</validValue>
      <validValue name="LIMIT">2</validValue>
      <validValue name="STOP">3</validValue>
      <validValue name="STOP_LIMIT">4</validValue>
      <validValue name="FUNARI">5</validValue>
      <validValue name="MOO">6</validValue>
      <validValue name="LOO">7</validValue>
    </enum>
    <enum name="TimeInForce" encodingType="uint8">
      <validValue name="DAY">1</validValue>
      <validValue name="IOC">2</validValue>
      <validValue name="FOK">3</validValue>
      <validValue name="GTC">4</validValue>
      <validValue name="GTD">5</validValue>
      <validValue name="OPG">6</validValue>
    </enum>
  </types>

  <sbe:message name="OrderSubmit" id="1">
    <field name="orderIdHigh" id="1" type="uint64"/>      <!-- UUIDv7 high bits -->
    <field name="orderIdLow"  id="2" type="uint64"/>      <!-- UUIDv7 low bits -->
    <field name="clOrdId"     id="3" type="char[32]"/>
    <field name="clientId"    id="4" type="uint64"/>
    <field name="instrumentIsin" id="5" type="char[12]"/>
    <field name="instrumentMic"  id="6" type="char[4]"/>
    <field name="side"        id="7" type="Side"/>
    <field name="ordType"     id="8" type="OrdType"/>
    <field name="quantity"    id="9" type="int64"/>      <!-- scaled by 1e8 -->
    <field name="price"       id="10" type="int64"/>     <!-- scaled by 1e8; INT64_MIN = market -->
    <field name="timeInForce" id="11" type="TimeInForce"/>
    <field name="bizTimeNanos" id="12" type="uint64"/>   <!-- nanoseconds since epoch -->
    <field name="region"      id="13" type="uint8"/>     <!-- 1=ZH, 2=LD4, 3=NY4, 4=TY3 -->
  </sbe:message>

  <sbe:message name="OrderAck" id="2">
    <field name="orderIdHigh" id="1" type="uint64"/>
    <field name="orderIdLow"  id="2" type="uint64"/>
    <field name="venueOrderId" id="3" type="char[32]"/>
    <field name="venueAckTimeNanos" id="4" type="uint64"/>
  </sbe:message>

  <sbe:message name="OrderCancel" id="3">
    <field name="orderIdHigh" id="1" type="uint64"/>
    <field name="orderIdLow"  id="2" type="uint64"/>
    <field name="origClOrdId" id="3" type="char[32]"/>
    <field name="bizTimeNanos" id="4" type="uint64"/>
  </sbe:message>

  <sbe:message name="OrderReplace" id="4">
    <!-- ... -->
  </sbe:message>

  <sbe:message name="RiskRejection" id="5">
    <field name="orderIdHigh" id="1" type="uint64"/>
    <field name="orderIdLow"  id="2" type="uint64"/>
    <field name="reason"      id="3" type="uint8"/>
    <field name="evaluationNanos" id="4" type="uint32"/>
  </sbe:message>
</sbe:message>
</sbe:messageSchema>
```

## Aeron channel naming

```
aeron:ipc?endpoint=acceptor-to-risk            (Stream 100, OrderSubmit/Cancel/Replace)
aeron:ipc?endpoint=risk-to-ems                 (Stream 101, OrderSubmit/Cancel/Replace post-risk)
aeron:ipc?endpoint=ems-to-acceptor             (Stream 102, OrderAck/RiskRejection back to client session)
```

## Property tests

`tests/property/java/sbe-orders-roundtrip-test.java` — jqwik generates random `OrderSubmit` payloads, encodes with the generated SBE encoder, decodes, and asserts equality of every field. Throughput benchmark in `tests/performance/jmh/SbeOrdersEncodeBench.java` enforces SC-equivalent SBE encode p99 < 100ns.
