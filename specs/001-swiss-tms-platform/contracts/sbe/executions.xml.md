# SBE Schema — Executions (internal hot-path)

**File**: `contracts/sbe/executions.xml`
**Schema id**: 2
**Used by**: `apps/venue-adapter-six/`, `apps/venue-adapter-eurex/`, `apps/ems-service/`, `apps/inbound-fix-acceptor/` (drop-copy back to client).

## Templates (excerpt)

```xml
<sbe:messageSchema package="ch.swisstms.sbe.executions" id="2" version="1" semanticVersion="1.0.0" byteOrder="littleEndian">
  <types>
    <enum name="ExecType" encodingType="uint8">
      <validValue name="NEW">1</validValue>
      <validValue name="PARTIAL_FILL">2</validValue>
      <validValue name="FILL">3</validValue>
      <validValue name="CANCELED">4</validValue>
      <validValue name="REPLACED">5</validValue>
      <validValue name="REJECTED">6</validValue>
      <validValue name="TRADE_BUST">7</validValue>
      <validValue name="EXPIRED">8</validValue>
    </enum>
    <enum name="LiquidityIndicator" encodingType="uint8">
      <validValue name="ADD">1</validValue>
      <validValue name="REMOVE">2</validValue>
      <validValue name="CROSSED">3</validValue>
      <validValue name="AUCTION">4</validValue>
    </enum>
  </types>

  <sbe:message name="ExecutionReport" id="1">
    <field name="orderIdHigh" id="1" type="uint64"/>
    <field name="orderIdLow"  id="2" type="uint64"/>
    <field name="executionIdHigh" id="3" type="uint64"/>
    <field name="executionIdLow"  id="4" type="uint64"/>
    <field name="venueExecutionId" id="5" type="char[32]"/>
    <field name="execType"   id="6" type="ExecType"/>
    <field name="quantity"   id="7" type="int64"/>     <!-- scaled by 1e8 -->
    <field name="price"      id="8" type="int64"/>
    <field name="cumQty"     id="9" type="int64"/>
    <field name="avgPx"      id="10" type="int64"/>
    <field name="leavesQty"  id="11" type="int64"/>
    <field name="liquidityIndicator" id="12" type="LiquidityIndicator"/>
    <field name="venueId"    id="13" type="char[4]"/>
    <field name="bizTimeNanos" id="14" type="uint64"/>
    <field name="procTimeNanos" id="15" type="uint64"/>
  </sbe:message>
</sbe:messageSchema>
```

## Aeron channel naming

```
aeron:ipc?endpoint=venue-to-ems-six          (Stream 200)
aeron:ipc?endpoint=venue-to-ems-eurex        (Stream 201)
aeron:ipc?endpoint=ems-to-acceptor-dropcopy  (Stream 210)
```

## Property tests

`tests/property/java/sbe-executions-roundtrip-test.java`. Throughput target identical to orders (SBE encode p99 < 100ns).
