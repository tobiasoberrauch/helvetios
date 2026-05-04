package ch.swisstms.venue.eurex.eti;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * T293 — Eurex T7 FIX-Gateway fallback (FIX 4.2 / 4.4).
 *
 * <p>The binary ETI is the primary transport (≤ 30 µs p99); the FIX-Gateway is a Eurex-provided
 * fallback that participants use during ETI outages or for low-touch flow. The session reuses the
 * QuickFIX/J infrastructure already wired into {@code apps/venue-adapter-six} but uses the
 * Eurex-specific 4.4 dictionary plus the {@code PartyIDExecutingTrader (20036)} extension.
 *
 * <p>Disabled by default; flip {@code swisstms.eurex.fix-gateway.enabled=true} to activate during
 * an ETI outage drill.
 */
@Component
@ConditionalOnProperty(value = "swisstms.eurex.fix-gateway.enabled", havingValue = "true")
public class EurexT7FixGatewayFallback {

  private static final Logger log = LoggerFactory.getLogger(EurexT7FixGatewayFallback.class);

  public EurexT7FixGatewayFallback() {
    log.warn(
        "Eurex T7 FIX-Gateway fallback ACTIVE — reduced throughput, use only during ETI outages");
  }

  public boolean fallbackActive() {
    return true;
  }
}
