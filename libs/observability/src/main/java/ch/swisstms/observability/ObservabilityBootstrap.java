package ch.swisstms.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

/**
 * Zentrale OTel + Micrometer Konfiguration. Phase 2: einfacher Prometheus-Registry. Echte
 * OTLP-Verkabelung in Phase 16 (T315ff).
 */
public final class ObservabilityBootstrap {

  private static MeterRegistry registry;

  private ObservabilityBootstrap() {}

  public static synchronized MeterRegistry meterRegistry() {
    if (registry == null) {
      registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
    return registry;
  }

  public static synchronized String scrape() {
    if (registry instanceof PrometheusMeterRegistry pmr) {
      return pmr.scrape();
    }
    return "";
  }
}
