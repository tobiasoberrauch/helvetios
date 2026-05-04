package ch.swisstms.oms.api;

import ch.swisstms.domain.client.ClientId;
import ch.swisstms.domain.common.Region;
import ch.swisstms.domain.instrument.InstrumentId;
import ch.swisstms.domain.order.AlgoStrategy;
import ch.swisstms.domain.order.ClOrdId;
import ch.swisstms.domain.order.OrdType;
import ch.swisstms.domain.order.OrderId;
import ch.swisstms.domain.order.RoutingMode;
import ch.swisstms.domain.order.Side;
import ch.swisstms.domain.order.TimeInForce;
import ch.swisstms.domain.price.Price;
import ch.swisstms.domain.price.Quantity;
import ch.swisstms.oms.application.OrderApplicationService;
import ch.swisstms.oms.application.SubmitOrderCommand;
import ch.swisstms.oms.infra.OrderEntity;
import ch.swisstms.oms.infra.OrderRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderApplicationService applicationService;
  private final OrderRepository orderRepository;
  private final UUID demoClientId;
  private final Region serviceRegion;

  public OrderController(
      OrderApplicationService applicationService,
      OrderRepository orderRepository,
      @Value("${swisstms.demo.client-id:00000000-0000-0000-0000-000000000001}") UUID demoClientId,
      @Value("${swisstms.region:ZH}") String region) {
    this.applicationService = applicationService;
    this.orderRepository = orderRepository;
    this.demoClientId = demoClientId;
    this.serviceRegion = Region.valueOf(region);
  }

  @PostMapping
  public ResponseEntity<OrderAckDto> submit(@Valid @RequestBody OrderRequestDto req) {
    Price price = req.price() == null ? Price.MARKET : Price.of(req.price());

    SubmitOrderCommand cmd =
        new SubmitOrderCommand(
            new ClOrdId(req.clOrdId()),
            new ClientId(demoClientId),
            serviceRegion,
            new InstrumentId(req.instrumentId().isin(), req.instrumentId().mic()),
            Side.valueOf(req.side()),
            OrdType.valueOf(req.ordType()),
            TimeInForce.valueOf(req.timeInForce()),
            Quantity.of(req.quantity()),
            price,
            RoutingMode.valueOf(req.routingMode()),
            req.algoStrategy() == null ? null : AlgoStrategy.valueOf(req.algoStrategy()),
            req.algoParameters(),
            req.preferredVenue(),
            req.expireTime());
    OrderId id = applicationService.submit(cmd);
    OrderEntity persisted = orderRepository.findById(id.value()).orElseThrow();
    OrderAckDto ack =
        new OrderAckDto(
            id.value(),
            req.clOrdId(),
            persisted.getSubmittedAtBiz(),
            persisted.getOrdStatus().name());
    return ResponseEntity.accepted().location(URI.create("/api/v1/orders/" + id.value())).body(ack);
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderDetailDto> get(@PathVariable UUID orderId) {
    Optional<OrderEntity> entity = orderRepository.findById(orderId);
    return entity
        .map(
            e ->
                ResponseEntity.ok(
                    new OrderDetailDto(
                        e.getOrderId(),
                        e.getClOrdId(),
                        e.getOrdStatus().name(),
                        e.getSide().name(),
                        e.getOrdType().name(),
                        e.getQuantity(),
                        e.getPrice(),
                        e.getCumQty(),
                        e.getLeavesQty(),
                        e.getAvgPx(),
                        e.getExecutionVenue(),
                        e.getSubmittedAtBiz(),
                        e.getLastUpdatedAt())))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping
  public List<OrderDetailDto> list() {
    return orderRepository.findAll().stream()
        .map(
            e ->
                new OrderDetailDto(
                    e.getOrderId(),
                    e.getClOrdId(),
                    e.getOrdStatus().name(),
                    e.getSide().name(),
                    e.getOrdType().name(),
                    e.getQuantity(),
                    e.getPrice(),
                    e.getCumQty(),
                    e.getLeavesQty(),
                    e.getAvgPx(),
                    e.getExecutionVenue(),
                    e.getSubmittedAtBiz(),
                    e.getLastUpdatedAt()))
        .toList();
  }
}
