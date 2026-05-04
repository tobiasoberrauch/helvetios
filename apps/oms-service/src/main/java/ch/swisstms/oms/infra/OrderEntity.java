package ch.swisstms.oms.infra;

import ch.swisstms.domain.common.Region;
import ch.swisstms.domain.order.AlgoStrategy;
import ch.swisstms.domain.order.OrdStatus;
import ch.swisstms.domain.order.OrdType;
import ch.swisstms.domain.order.RoutingMode;
import ch.swisstms.domain.order.Side;
import ch.swisstms.domain.order.TimeInForce;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_aggregate")
public class OrderEntity {

  @Id
  @Column(name = "order_id")
  private UUID orderId;

  @Column(name = "cl_ord_id", nullable = false)
  private String clOrdId;

  @Column(name = "orig_cl_ord_id")
  private String origClOrdId;

  @Column(name = "client_id", nullable = false)
  private UUID clientId;

  @Column(name = "trader_id")
  private UUID traderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "region", nullable = false, columnDefinition = "region_enum")
  private Region region;

  @Column(name = "instrument_isin", nullable = false, length = 12)
  private String instrumentIsin;

  @Column(name = "instrument_mic", nullable = false, length = 4)
  private String instrumentMic;

  @Enumerated(EnumType.STRING)
  @Column(name = "side", nullable = false, columnDefinition = "side_enum")
  private Side side;

  @Enumerated(EnumType.STRING)
  @Column(name = "ord_type", nullable = false, columnDefinition = "ord_type_enum")
  private OrdType ordType;

  @Enumerated(EnumType.STRING)
  @Column(name = "time_in_force", nullable = false, columnDefinition = "tif_enum")
  private TimeInForce timeInForce;

  @Column(name = "expire_time")
  private Instant expireTime;

  @Column(name = "quantity", nullable = false, precision = 18, scale = 8)
  private BigDecimal quantity;

  @Column(name = "price", precision = 18, scale = 8)
  private BigDecimal price;

  @Enumerated(EnumType.STRING)
  @Column(name = "routing_mode", nullable = false, columnDefinition = "routing_mode_enum")
  private RoutingMode routingMode;

  @Enumerated(EnumType.STRING)
  @Column(name = "algo_strategy", columnDefinition = "algo_strategy_enum")
  private AlgoStrategy algoStrategy;

  @Enumerated(EnumType.STRING)
  @Column(name = "ord_status", nullable = false, columnDefinition = "ord_status_enum")
  private OrdStatus ordStatus;

  @Column(name = "cum_qty", nullable = false, precision = 18, scale = 8)
  private BigDecimal cumQty = BigDecimal.ZERO;

  @Column(name = "leaves_qty", nullable = false, precision = 18, scale = 8)
  private BigDecimal leavesQty;

  @Column(name = "avg_px", precision = 18, scale = 8)
  private BigDecimal avgPx;

  @Column(name = "submitted_at_biz", nullable = false)
  private Instant submittedAtBiz;

  @Column(name = "submitted_at_proc", nullable = false)
  private Instant submittedAtProc;

  @Column(name = "last_updated_at", nullable = false)
  private Instant lastUpdatedAt;

  @Column(name = "preferred_venue", length = 4)
  private String preferredVenue;

  @Column(name = "execution_venue", length = 4)
  private String executionVenue;

  @Version
  @Column(name = "version")
  private Long version;

  protected OrderEntity() {
    /* JPA */
  }

  public static OrderEntity newOrder(
      UUID orderId,
      String clOrdId,
      UUID clientId,
      Region region,
      String isin,
      String mic,
      Side side,
      OrdType ordType,
      TimeInForce tif,
      BigDecimal quantity,
      BigDecimal price,
      RoutingMode routingMode,
      Instant submittedAtBiz,
      Instant submittedAtProc) {
    OrderEntity e = new OrderEntity();
    e.orderId = orderId;
    e.clOrdId = clOrdId;
    e.clientId = clientId;
    e.region = region;
    e.instrumentIsin = isin;
    e.instrumentMic = mic;
    e.side = side;
    e.ordType = ordType;
    e.timeInForce = tif;
    e.quantity = quantity;
    e.price = price;
    e.routingMode = routingMode;
    e.ordStatus = OrdStatus.NEW;
    e.cumQty = BigDecimal.ZERO;
    e.leavesQty = quantity;
    e.submittedAtBiz = submittedAtBiz;
    e.submittedAtProc = submittedAtProc;
    e.lastUpdatedAt = submittedAtProc;
    return e;
  }

  // ----- Getters / setters (the boring part) -----------------------------

  public UUID getOrderId() {
    return orderId;
  }

  public String getClOrdId() {
    return clOrdId;
  }

  public String getOrigClOrdId() {
    return origClOrdId;
  }

  public UUID getClientId() {
    return clientId;
  }

  public Region getRegion() {
    return region;
  }

  public String getInstrumentIsin() {
    return instrumentIsin;
  }

  public String getInstrumentMic() {
    return instrumentMic;
  }

  public Side getSide() {
    return side;
  }

  public OrdType getOrdType() {
    return ordType;
  }

  public TimeInForce getTimeInForce() {
    return timeInForce;
  }

  public Instant getExpireTime() {
    return expireTime;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public RoutingMode getRoutingMode() {
    return routingMode;
  }

  public AlgoStrategy getAlgoStrategy() {
    return algoStrategy;
  }

  public OrdStatus getOrdStatus() {
    return ordStatus;
  }

  public BigDecimal getCumQty() {
    return cumQty;
  }

  public BigDecimal getLeavesQty() {
    return leavesQty;
  }

  public BigDecimal getAvgPx() {
    return avgPx;
  }

  public Instant getSubmittedAtBiz() {
    return submittedAtBiz;
  }

  public Instant getSubmittedAtProc() {
    return submittedAtProc;
  }

  public Instant getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public String getPreferredVenue() {
    return preferredVenue;
  }

  public String getExecutionVenue() {
    return executionVenue;
  }

  public void setOrdStatus(OrdStatus s) {
    this.ordStatus = s;
  }

  public void setCumQty(BigDecimal q) {
    this.cumQty = q;
  }

  public void setLeavesQty(BigDecimal q) {
    this.leavesQty = q;
  }

  public void setAvgPx(BigDecimal p) {
    this.avgPx = p;
  }

  public void setLastUpdatedAt(Instant t) {
    this.lastUpdatedAt = t;
  }

  public void setExecutionVenue(String v) {
    this.executionVenue = v;
  }

  public void setOrigClOrdId(String s) {
    this.origClOrdId = s;
  }

  public void setQuantity(BigDecimal q) {
    this.quantity = q;
  }

  public void setPrice(BigDecimal p) {
    this.price = p;
  }

  public void setPreferredVenue(String v) {
    this.preferredVenue = v;
  }
}
