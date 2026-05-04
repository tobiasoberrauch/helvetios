package ch.swisstms.oms.infra;

import ch.swisstms.domain.execution.ExecType;
import ch.swisstms.domain.execution.ExecutionReport;
import ch.swisstms.domain.ports.VenueGatewayPort;
import ch.swisstms.oms.application.OrderApplicationService;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Bridges every venue adapter's {@code Flow.Publisher<ExecutionReport>} into the OMS {@link
 * OrderApplicationService}. One subscriber per adapter; runs lifecycle-managed by Spring.
 */
@Component
public class VenueExecutionListener {

  private static final Logger log = LoggerFactory.getLogger(VenueExecutionListener.class);

  private final List<VenueGatewayPort> adapters;
  private final OrderApplicationService applicationService;

  public VenueExecutionListener(
      List<VenueGatewayPort> adapters, OrderApplicationService applicationService) {
    this.adapters = adapters;
    this.applicationService = applicationService;
  }

  @PostConstruct
  public void subscribeAll() {
    for (VenueGatewayPort adapter : adapters) {
      adapter
          .executions()
          .subscribe(
              new Subscriber<>() {
                Subscription subscription;

                @Override
                public void onSubscribe(Subscription s) {
                  this.subscription = s;
                  s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(ExecutionReport report) {
                  try {
                    if (report.execType() == ExecType.NEW) {
                      applicationService.acknowledge(
                          report.orderId().value(), report.venueExecutionId(), report.bizTime());
                    } else if (report.execType() == ExecType.PARTIAL_FILL
                        || report.execType() == ExecType.FILL) {
                      applicationService.applyExecution(report);
                    } else {
                      log.debug(
                          "Ignoring execType {} for order {}", report.execType(), report.orderId());
                    }
                  } catch (Exception e) {
                    log.error(
                        "Failed to apply execution {} from {}",
                        report.executionId(),
                        adapter.venueMic(),
                        e);
                  }
                }

                @Override
                public void onError(Throwable err) {
                  log.error("Execution stream from {} errored", adapter.venueMic(), err);
                }

                @Override
                public void onComplete() {
                  log.info("Execution stream from {} completed", adapter.venueMic());
                }
              });
      log.info("Subscribed to executions from venue adapter {}", adapter.venueMic());
    }
  }
}
