package ch.swisstms.oms.api;

import java.time.Instant;
import java.util.UUID;

public record OrderAckDto(UUID orderId, String clOrdId, Instant submittedAtBiz, String status) {}
