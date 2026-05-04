package ch.swisstms.venue.bloomberg.emsx;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bloomberg EMSX-API client (T179).
 *
 * <p>Service: {@code //blp/emapisvc} (production) / {@code //blp/emapisvc_beta} (UAT). Drives the
 * Bloomberg trader workstation: CreateOrderAndRouteEx, RouteEx, ModifyRouteEx, CancelRouteEx,
 * AssignTrader; subscriptions OrderSubscription / RouteSubscription stream lifecycle events back.
 *
 * <p>Phase 8 ships the request surface so the OMS can reach EMSX via {@link CompletionStage}; Phase
 * 14 wires the real BLPAPI calls.
 */
@Component
public class EmsxClient {

  private static final Logger log = LoggerFactory.getLogger(EmsxClient.class);

  private final String serviceName;

  public EmsxClient(@Value("${swisstms.bloomberg.emsx.service:/blp/emapisvc_beta}") String svc) {
    this.serviceName = svc;
  }

  public record OrderRouteRequest(
      String trader, String account, String ticker, long qty, String side, String routeBroker) {}

  public record OrderRouteAck(String orderId, String routeId, String status) {}

  public CompletionStage<OrderRouteAck> createOrderAndRouteEx(OrderRouteRequest req) {
    log.info(
        "EMSX [{}] CreateOrderAndRouteEx trader={} ticker={} qty={} side={} broker={}",
        serviceName,
        req.trader(),
        req.ticker(),
        req.qty(),
        req.side(),
        req.routeBroker());
    return CompletableFuture.completedFuture(
        new OrderRouteAck("ORD-" + UUID.randomUUID(), "RTE-" + UUID.randomUUID(), "ACCEPTED"));
  }

  public CompletionStage<Void> cancelRouteEx(String orderId, String routeId) {
    log.info("EMSX [{}] CancelRouteEx order={} route={}", serviceName, orderId, routeId);
    return CompletableFuture.completedFuture(null);
  }
}
