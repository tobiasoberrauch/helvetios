package ch.swisstms.inbound.fix;

import ch.swisstms.domain.order.RoutingMode;
import org.springframework.stereotype.Component;

/**
 * FIX HandlInst (Tag 21) → domain {@link RoutingMode} mapper (T245 / R-020).
 *
 * <p>FIX HandlInst values:
 *
 * <ul>
 *   <li>{@code 1} — Automated execution, broker-intervention OK ⇒ {@link RoutingMode#DMA}
 *   <li>{@code 2} — Automated execution, no broker-intervention ⇒ {@link RoutingMode#ALGO_WHEEL}
 *   <li>{@code 3} — Manual order, best execution ⇒ {@link RoutingMode#CARE}
 * </ul>
 *
 * <p>Anything else triggers a {@code BusinessMessageReject(35=j)} via the {@code
 * FixRejectGenerator} — the acceptor never silently maps unknown values.
 */
@Component
public class HandlInstMapper {

  public RoutingMode toRoutingMode(char handlInst) {
    return switch (handlInst) {
      case '1' -> RoutingMode.DMA;
      case '2' -> RoutingMode.ALGO_WHEEL;
      case '3' -> RoutingMode.CARE;
      default -> throw new IllegalArgumentException("Unsupported HandlInst value: " + handlInst);
    };
  }

  public char toFixValue(RoutingMode mode) {
    return switch (mode) {
      case DMA -> '1';
      case ALGO_WHEEL -> '2';
      case CARE -> '3';
    };
  }
}
