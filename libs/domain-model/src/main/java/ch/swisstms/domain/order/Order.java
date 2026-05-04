package ch.swisstms.domain.order;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.common.Region;
import ch.swisstms.domain.execution.ExecutionReport;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Order aggregate root. State changes go through one of: submit() / acknowledge() /
 * applyExecution() / cancel() / replace() / reject() / expire().
 *
 * <p>Constitution Principle II — every state transition is gated by {@link
 * OrdStatus#canTransitionTo}.
 */
public final class Order {

  private final OrderId orderId;
  private final ClOrdId clOrdId;
  private ClOrdId origClOrdId;
  private final ClientId clientId;
  private final Region region;
  private final InstrumentId instrument;
  private final Side side;
  private final OrdType ordType;
  private final TimeInForce timeInForce;
  private final RoutingMode routingMode;
  private Quantity orderQty;
  private Price price;
  private Quantity cumQty = Quantity.ZERO;
  private Quantity leavesQty;
  private Price avgPx = Price.ZERO;
  private OrdStatus status = OrdStatus.NEW;
  private final Instant submittedAtBiz;
  private Instant lastUpdatedAtBiz;
  private String executionVenue; // immutable once set on first fill

  public Order(
      OrderId orderId,
      ClOrdId clOrdId,
      ClientId clientId,
      Region region,
      InstrumentId instrument,
      Side side,
      OrdType ordType,
      TimeInForce timeInForce,
      RoutingMode routingMode,
      Quantity orderQty,
      Price price,
      Instant submittedAtBiz) {
    this.orderId = Objects.requireNonNull(orderId);
    this.clOrdId = Objects.requireNonNull(clOrdId);
    this.clientId = Objects.requireNonNull(clientId);
    this.region = Objects.requireNonNull(region);
    this.instrument = Objects.requireNonNull(instrument);
    this.side = Objects.requireNonNull(side);
    this.ordType = Objects.requireNonNull(ordType);
    this.timeInForce = Objects.requireNonNull(timeInForce);
    this.routingMode = Objects.requireNonNull(routingMode);
    this.orderQty = Objects.requireNonNull(orderQty);
    this.price = price; // may be Price.MARKET for MARKET orders
    if (orderQty.isZero()) {
      throw new IllegalArgumentException("orderQty must be positive");
    }
    if (ordType != OrdType.MARKET && (price == null || price.isMarket())) {
      throw new IllegalArgumentException("non-MARKET order requires a price");
    }
    this.leavesQty = orderQty;
    this.submittedAtBiz = Objects.requireNonNull(submittedAtBiz);
    this.lastUpdatedAtBiz = submittedAtBiz;
  }

  // ----- Lifecycle commands ---------------------------------------------

  public void acknowledge(Instant atBiz) {
    transition(OrdStatus.ACKNOWLEDGED, atBiz);
  }

  public void reject(Instant atBiz) {
    transition(OrdStatus.REJECTED, atBiz);
  }

  public void businessReject(Instant atBiz) {
    transition(OrdStatus.BUSINESS_REJECTED, atBiz);
  }

  public void expire(Instant atBiz) {
    transition(OrdStatus.EXPIRED, atBiz);
  }

  public void requestCancel(Instant atBiz) {
    transition(OrdStatus.PENDING_CANCEL, atBiz);
  }

  public void cancelConfirmed(Instant atBiz) {
    transition(OrdStatus.CANCELLED, atBiz);
  }

  public void requestReplace(Instant atBiz) {
    transition(OrdStatus.PENDING_REPLACE, atBiz);
  }

  public void replaceConfirmed(Quantity newQty, Price newPx, Instant atBiz) {
    transition(OrdStatus.ACKNOWLEDGED, atBiz);
    if (newQty != null) {
      if (newQty.compareTo(cumQty) <= 0) {
        throw new IllegalArgumentException("Replaced quantity must exceed already-filled cumQty");
      }
      this.orderQty = newQty;
      this.leavesQty = newQty.minus(cumQty);
    }
    if (newPx != null) {
      this.price = newPx;
    }
  }

  public void tradeBust(Instant atBiz) {
    transition(OrdStatus.TRADE_BUSTED, atBiz);
  }

  public void applyExecution(ExecutionReport report) {
    if (report.execType() != ch.swisstms.domain.execution.ExecType.PARTIAL_FILL
        && report.execType() != ch.swisstms.domain.execution.ExecType.FILL) {
      throw new IllegalArgumentException(
          "applyExecution accepts only PARTIAL_FILL or FILL, got " + report.execType());
    }
    // Constitution Principle V — drop-copy is the source of truth; OMS
    // accepts what the post-recon stream tells it. Authority is upstream.

    Quantity fillQty = report.quantity();
    if (fillQty.isZero() || fillQty.compareTo(leavesQty) > 0) {
      throw new IllegalStateException(
          "Fill quantity %s exceeds leavesQty %s on order %s"
              .formatted(fillQty, leavesQty, orderId));
    }
    Price fillPx = report.price();
    if (fillPx == null || fillPx.isMarket()) {
      throw new IllegalStateException("Fill must carry a numeric price");
    }
    // Update cumulative fields
    BigDecimal newCumQty = cumQty.toBigDecimal().add(fillQty.toBigDecimal());
    // VWAP: sum(qty * px) / sum(qty)
    BigDecimal weightedSum =
        (avgPx.equals(Price.ZERO)
                ? BigDecimal.ZERO
                : avgPx.toBigDecimal().multiply(cumQty.toBigDecimal()))
            .add(fillPx.toBigDecimal().multiply(fillQty.toBigDecimal()));
    BigDecimal newAvgPx =
        newCumQty.signum() == 0
            ? BigDecimal.ZERO
            : weightedSum.divide(newCumQty, Price.SCALE, RoundingMode.HALF_EVEN);

    this.cumQty = Quantity.of(newCumQty);
    this.leavesQty = orderQty.minus(this.cumQty);
    this.avgPx = Price.of(newAvgPx);
    if (this.executionVenue == null) {
      this.executionVenue = report.venueId();
    }

    OrdStatus next = leavesQty.isZero() ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED;
    transition(next, report.bizTime());
  }

  private void transition(OrdStatus next, Instant atBiz) {
    this.status = this.status.transitionTo(next);
    this.lastUpdatedAtBiz = atBiz;
  }

  // ----- Accessors ------------------------------------------------------

  public OrderId orderId() {
    return orderId;
  }

  public ClOrdId clOrdId() {
    return clOrdId;
  }

  public ClientId clientId() {
    return clientId;
  }

  public Region region() {
    return region;
  }

  public InstrumentId instrument() {
    return instrument;
  }

  public Side side() {
    return side;
  }

  public OrdType ordType() {
    return ordType;
  }

  public TimeInForce timeInForce() {
    return timeInForce;
  }

  public RoutingMode routingMode() {
    return routingMode;
  }

  public Quantity orderQty() {
    return orderQty;
  }

  public Price price() {
    return price;
  }

  public Quantity cumQty() {
    return cumQty;
  }

  public Quantity leavesQty() {
    return leavesQty;
  }

  public Price avgPx() {
    return avgPx;
  }

  public OrdStatus status() {
    return status;
  }

  public Instant submittedAtBiz() {
    return submittedAtBiz;
  }

  public Instant lastUpdatedAtBiz() {
    return lastUpdatedAtBiz;
  }

  public String executionVenue() {
    return executionVenue;
  }
}
