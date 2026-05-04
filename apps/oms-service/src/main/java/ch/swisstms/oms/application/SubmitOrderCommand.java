package ch.swisstms.oms.application;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.common.Region;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.order.AlgoStrategy;
import ch.swisstms.domain.order.ClOrdId;
import ch.swisstms.domain.order.OrdType;
import ch.swisstms.domain.order.RoutingMode;
import ch.swisstms.domain.order.Side;
import ch.swisstms.domain.order.TimeInForce;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.time.Instant;
import java.util.Map;

public record SubmitOrderCommand(
    ClOrdId clOrdId,
    ClientId clientId,
    Region region,
    InstrumentId instrument,
    Side side,
    OrdType ordType,
    TimeInForce timeInForce,
    Quantity quantity,
    Price price,
    RoutingMode routingMode,
    AlgoStrategy algoStrategy,
    Map<String, String> algoParameters,
    String preferredVenue,
    Instant expireTime) {}
