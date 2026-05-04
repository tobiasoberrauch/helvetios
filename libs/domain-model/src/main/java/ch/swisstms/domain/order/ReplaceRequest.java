package ch.swisstms.domain.order;

import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.time.Instant;

public record ReplaceRequest(
    OrderId orderId,
    ClOrdId origClOrdId,
    ClOrdId clOrdId,
    Quantity newQuantity,
    Price newPrice,
    Instant transactTime) {}
