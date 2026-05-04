# SBE Schema — Market Data (internal hot-path)

**File**: `contracts/sbe/market-data.xml`
**Schema id**: 3
**Used by**: `apps/market-data-service/` (publisher), hot-path consumers (`apps/ems-service/`, `apps/pretrade-risk-gateway/`'s reference price cache, `apps/venue-adapter-*/` for price-based routing).

## Templates (excerpt)

```xml
<sbe:messageSchema package="ch.swisstms.sbe.marketdata" id="3" version="1" semanticVersion="1.0.0" byteOrder="littleEndian">
  <sbe:message name="QuoteUpdate" id="1">
    <field name="instrumentIsin" id="1" type="char[12]"/>
    <field name="instrumentMic"  id="2" type="char[4]"/>
    <field name="bidPrice"   id="3" type="int64"/>     <!-- scaled by 1e8 -->
    <field name="bidQty"     id="4" type="int64"/>
    <field name="askPrice"   id="5" type="int64"/>
    <field name="askQty"     id="6" type="int64"/>
    <field name="bizTimeNanos" id="7" type="uint64"/>
    <field name="source"     id="8" type="char[8]"/>   <!-- e.g., 'IMI', 'EMA', 'BPIPE' -->
    <field name="seqNumber"  id="9" type="uint64"/>
  </sbe:message>

  <sbe:message name="TradeUpdate" id="2">
    <field name="instrumentIsin" id="1" type="char[12]"/>
    <field name="instrumentMic"  id="2" type="char[4]"/>
    <field name="price"      id="3" type="int64"/>
    <field name="quantity"   id="4" type="int64"/>
    <field name="bizTimeNanos" id="5" type="uint64"/>
    <field name="source"     id="6" type="char[8]"/>
    <field name="seqNumber"  id="7" type="uint64"/>
  </sbe:message>

  <sbe:message name="BookSnapshot" id="3">
    <field name="instrumentIsin" id="1" type="char[12]"/>
    <field name="instrumentMic"  id="2" type="char[4]"/>
    <group name="bidLevels" id="4" dimensionType="groupSizeEncoding">
      <field name="price" id="1" type="int64"/>
      <field name="qty"   id="2" type="int64"/>
      <field name="orderCount" id="3" type="uint32"/>
    </group>
    <group name="askLevels" id="5" dimensionType="groupSizeEncoding">
      <field name="price" id="1" type="int64"/>
      <field name="qty"   id="2" type="int64"/>
      <field name="orderCount" id="3" type="uint32"/>
    </group>
    <field name="bizTimeNanos" id="6" type="uint64"/>
    <field name="source"     id="7" type="char[8]"/>
    <field name="seqNumber"  id="8" type="uint64"/>
  </sbe:message>
</sbe:messageSchema>
```

## Aeron channels

Multicast UDP for high-fan-out. One channel per instrument-class to keep volume per channel manageable.

```
aeron:udp?endpoint=224.10.0.1:40456|interface=eth0     (Stream 300, equity quotes)
aeron:udp?endpoint=224.10.0.2:40457                    (Stream 301, listed-derivative quotes)
aeron:udp?endpoint=224.10.0.3:40458                    (Stream 302, FX quotes)
aeron:udp?endpoint=224.10.0.4:40459                    (Stream 310, equity trades)
... etc
```

## Sequence-gap detection

`seqNumber` per `(instrument, source)`. Consumers maintain a `Long2LongHashMap` of last-seen seq and emit a `cold.recon.gap-detected.v1` Kafka event on any gap.
