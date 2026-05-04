package ch.swisstms.oms.application;

import ch.swisstms.domain.execution.ExecutionReport;
import ch.swisstms.domain.health.LatencyTier;
import ch.swisstms.domain.order.OrdStatus;
import ch.swisstms.domain.order.OrderId;
import ch.swisstms.domain.ports.VenueGatewayPort;
import ch.swisstms.oms.infra.OrderEntity;
import ch.swisstms.oms.infra.OrderEventEntity;
import ch.swisstms.oms.infra.OrderEventRepository;
import ch.swisstms.oms.infra.OrderRepository;
import ch.swisstms.oms.infra.OutboxEntity;
import ch.swisstms.oms.infra.OutboxRepository;
import ch.swisstms.oms.ports.VenueRouter;
import ch.swisstms.time_sync.RegulatoryClock;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anwendungs-Logik der OMS-Aggregate. Jede State-änderung schreibt (a) einen Event-Store-Eintrag
 * mit Hash-Chain (Constitution VI) (b) einen Outbox-Eintrag (Topic `cold.oms.event.v1`).
 */
@Service
public class OrderApplicationService {

  private static final Logger log = LoggerFactory.getLogger(OrderApplicationService.class);
  private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();

  private final OrderRepository orderRepository;
  private final OrderEventRepository eventRepository;
  private final OutboxRepository outboxRepository;
  private final VenueRouter venueRouter;
  private final String omsTopic;

  public OrderApplicationService(
      OrderRepository orderRepository,
      OrderEventRepository eventRepository,
      OutboxRepository outboxRepository,
      VenueRouter venueRouter,
      @Value("${swisstms.topics.oms-event:cold.oms.event.v1}") String omsTopic) {
    this.orderRepository = orderRepository;
    this.eventRepository = eventRepository;
    this.outboxRepository = outboxRepository;
    this.venueRouter = venueRouter;
    this.omsTopic = omsTopic;
  }

  @Transactional
  public OrderId submit(SubmitOrderCommand cmd) {
    UUID orderUuid = UUID.randomUUID();
    Instant bizTime = RegulatoryClock.nowBiz();
    Instant procTime = bizTime;

    OrderEntity entity =
        OrderEntity.newOrder(
            orderUuid,
            cmd.clOrdId().value(),
            cmd.clientId().value(),
            cmd.region(),
            cmd.instrument().isin(),
            cmd.instrument().mic(),
            cmd.side(),
            cmd.ordType(),
            cmd.timeInForce(),
            cmd.quantity().toBigDecimal(),
            cmd.price() == null || cmd.price().isMarket() ? null : cmd.price().toBigDecimal(),
            cmd.routingMode(),
            bizTime,
            procTime);
    if (cmd.preferredVenue() != null) {
      entity.setPreferredVenue(cmd.preferredVenue());
    }
    orderRepository.save(entity);
    appendEvent(
        entity,
        OrdStatus.NEW,
        "ORDER_SUBMITTED",
        Map.of(
            "clOrdId", cmd.clOrdId().value(),
            "side", cmd.side().name(),
            "ordType", cmd.ordType().name(),
            "instrument", Map.of("isin", cmd.instrument().isin(), "mic", cmd.instrument().mic())),
        bizTime);

    // Forward to venue (asynchronously). For US1 this is a best-effort
    // fire-and-forget; the venue's drop-copy + reconciler is the
    // authoritative path (Constitution V).
    String mic = cmd.preferredVenue() != null ? cmd.preferredVenue() : cmd.instrument().mic();
    venueRouter
        .resolve(mic)
        .ifPresentOrElse(
            adapter -> dispatchToVenue(adapter, entity, cmd),
            () ->
                log.warn("No venue adapter for MIC={}, order {} will sit pending", mic, orderUuid));

    return new OrderId(orderUuid);
  }

  private void dispatchToVenue(
      VenueGatewayPort adapter, OrderEntity entity, SubmitOrderCommand cmd) {
    if (adapter.tier() == LatencyTier.HOT) {
      // Hot-tier adapters are addressed via Aeron from the EMS, not from
      // a Spring transactional service. Phase 13 routes hot orders
      // through the inbound-fix-acceptor + pretrade-risk-gateway.
      log.debug("Hot-tier adapter {} — handing off to EMS via Aeron", adapter.venueMic());
      return;
    }
    // For Phase 3 (US1) the SIX STI adapter advertises WARM tier and
    // accepts a domain Order directly. We rebuild the domain Order from
    // the entity and submit asynchronously.
    ch.swisstms.domain.order.Order domainOrder = rebuildDomainOrder(entity);
    adapter
        .submitOrder(domainOrder)
        .whenComplete(
            (ack, err) -> {
              if (err != null) {
                log.error(
                    "Venue submit failed for order {} on MIC {}",
                    entity.getOrderId(),
                    adapter.venueMic(),
                    err);
              } else {
                log.info(
                    "Venue {} acknowledged order {}: venueOrderId={}",
                    adapter.venueMic(),
                    entity.getOrderId(),
                    ack.venueOrderId());
              }
            });
  }

  private static ch.swisstms.domain.order.Order rebuildDomainOrder(OrderEntity e) {
    ch.swisstms.domain.price.Price px =
        e.getPrice() == null
            ? ch.swisstms.domain.price.Price.MARKET
            : ch.swisstms.domain.price.Price.of(e.getPrice());
    return new ch.swisstms.domain.order.Order(
        new ch.swisstms.domain.order.OrderId(e.getOrderId()),
        new ch.swisstms.domain.order.ClOrdId(e.getClOrdId()),
        new ch.swisstms.domain.client.ClientId(e.getClientId()),
        e.getRegion(),
        new ch.swisstms.domain.instrument.InstrumentId(e.getInstrumentIsin(), e.getInstrumentMic()),
        e.getSide(),
        e.getOrdType(),
        e.getTimeInForce(),
        e.getRoutingMode(),
        ch.swisstms.domain.price.Quantity.of(e.getQuantity()),
        px,
        e.getSubmittedAtBiz());
  }

  @Transactional
  public void applyExecution(ExecutionReport report) {
    OrderEntity entity =
        orderRepository
            .findById(report.orderId().value())
            .orElseThrow(() -> new IllegalStateException("Unknown orderId: " + report.orderId()));

    entity.setCumQty(report.cumQty().toBigDecimal());
    entity.setLeavesQty(report.leavesQty().toBigDecimal());
    if (report.avgPx() != null && !report.avgPx().isMarket()) {
      entity.setAvgPx(report.avgPx().toBigDecimal());
    }
    if (entity.getExecutionVenue() == null && report.venueId() != null) {
      entity.setExecutionVenue(report.venueId());
    }
    OrdStatus next =
        entity.getLeavesQty().signum() == 0 ? OrdStatus.FILLED : OrdStatus.PARTIALLY_FILLED;
    OrdStatus current = entity.getOrdStatus();
    if (!current.canTransitionTo(next) && current != next) {
      // Constitution V — drop-copy is the source of truth. If we're
      // already in a state that can't legally transition, log and
      // leave the OMS to be reconciled.
      log.warn(
          "Suppressing execution apply: order {} status {} cannot accept fill",
          entity.getOrderId(),
          current);
      return;
    }
    if (current.canTransitionTo(next)) {
      entity.setOrdStatus(next);
    }
    entity.setLastUpdatedAt(report.bizTime());

    String eventType = next == OrdStatus.FILLED ? "ORDER_FILLED" : "ORDER_PARTIALLY_FILLED";
    appendEvent(
        entity,
        next,
        eventType,
        Map.of(
            "executionId", report.executionId().toString(),
            "venueExecutionId", report.venueExecutionId(),
            "execType", report.execType().name(),
            "quantity", report.quantity().toString(),
            "price", report.price().toString(),
            "cumQty", report.cumQty().toString(),
            "leavesQty", report.leavesQty().toString(),
            "venueId", report.venueId() == null ? "" : report.venueId()),
        report.bizTime());
  }

  @Transactional
  public void acknowledge(UUID orderId, String venueOrderId, Instant bizTime) {
    OrderEntity entity =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new IllegalStateException("Unknown orderId: " + orderId));
    if (entity.getOrdStatus().canTransitionTo(OrdStatus.ACKNOWLEDGED)) {
      entity.setOrdStatus(OrdStatus.ACKNOWLEDGED);
      entity.setLastUpdatedAt(bizTime);
      appendEvent(
          entity,
          OrdStatus.ACKNOWLEDGED,
          "ORDER_ACKED",
          Map.of("venueOrderId", venueOrderId),
          bizTime);
    }
  }

  /** Append an event to the order_event store with hash-chain link. */
  private void appendEvent(
      OrderEntity entity,
      OrdStatus status,
      String eventType,
      Map<String, ?> payload,
      Instant bizTime) {
    long nextSeq =
        eventRepository
            .findTop1ByOrderIdOrderBySeqDesc(entity.getOrderId())
            .map(e -> e.getSeq() + 1)
            .orElse(1L);
    byte[] prevHash =
        nextSeq == 1
            ? new byte[32]
            : eventRepository
                .findTop1ByOrderIdOrderBySeqDesc(entity.getOrderId())
                .map(OrderEventEntity::getHash)
                .orElse(new byte[32]);

    Map<String, Object> envelope =
        Map.of(
            "orderId", entity.getOrderId().toString(),
            "status", status.name(),
            "payload", payload);
    String json;
    try {
      json = JSON.writeValueAsString(envelope);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to serialise event payload", e);
    }

    byte[] hash = sha256(prevHash, json.getBytes());
    Instant procTime = RegulatoryClock.nowBiz();
    OrderEventEntity event =
        new OrderEventEntity(
            UUID.randomUUID(),
            entity.getOrderId(),
            nextSeq,
            eventType,
            json,
            bizTime,
            procTime,
            prevHash,
            hash,
            null);
    eventRepository.save(event);

    outboxRepository.save(
        new OutboxEntity(UUID.randomUUID(), entity.getOrderId(), omsTopic, json, null));
  }

  private static byte[] sha256(byte[] prev, byte[] payload) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(prev);
      md.update(payload);
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
