package ch.swisstms.domain.marketdata;

import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.time.Instant;

public record MarketDataTick(
    InstrumentId instrument,
    Price bidPrice,
    Quantity bidQty,
    Price askPrice,
    Quantity askQty,
    Price lastPrice,
    Quantity lastQty,
    Instant bizTime,
    String source,
    long sequenceNumber) {}
