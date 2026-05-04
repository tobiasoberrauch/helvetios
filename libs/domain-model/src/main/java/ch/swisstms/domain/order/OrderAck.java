package ch.swisstms.domain.order;

import java.time.Instant;

public record OrderAck(OrderId orderId, String venueOrderId, Instant venueAckTime) {}
