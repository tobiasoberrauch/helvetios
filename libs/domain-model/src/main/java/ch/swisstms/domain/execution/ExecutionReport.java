package ch.swisstms.domain.execution;

import ch.swisstms.domain.order.OrderId;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.time.Instant;
import java.util.Objects;

/**
 * Domain-level execution report — venue-protocol-neutral. Adapters translate FIX 35=8 / SBE
 * ExecutionReport / BLPAPI EMSX events into this type.
 */
public record ExecutionReport(
    ExecutionId executionId,
    String venueExecutionId,
    OrderId orderId,
    ExecType execType,
    Quantity quantity,
    Price price,
    Quantity cumQty,
    Quantity leavesQty,
    Price avgPx,
    LiquidityIndicator liquidityIndicator,
    String venueId,
    Instant bizTime,
    Instant procTime) {
  public ExecutionReport {
    Objects.requireNonNull(executionId);
    Objects.requireNonNull(orderId);
    Objects.requireNonNull(execType);
    Objects.requireNonNull(quantity);
    Objects.requireNonNull(cumQty);
    Objects.requireNonNull(leavesQty);
    Objects.requireNonNull(bizTime);
  }
}
