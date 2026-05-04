package ch.swisstms.domain.order;

import java.time.Instant;

public record CancelRequest(
    OrderId orderId, ClOrdId origClOrdId, ClOrdId clOrdId, Instant transactTime) {}
