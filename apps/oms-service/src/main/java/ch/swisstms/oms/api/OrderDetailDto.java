package ch.swisstms.oms.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderDetailDto(
    UUID orderId,
    String clOrdId,
    String status,
    String side,
    String ordType,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal cumQty,
    BigDecimal leavesQty,
    BigDecimal avgPx,
    String executionVenue,
    Instant submittedAtBiz,
    Instant lastUpdatedAt) {}
