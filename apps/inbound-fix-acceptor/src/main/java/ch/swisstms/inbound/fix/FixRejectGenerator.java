package ch.swisstms.inbound.fix;

import ch.swisstms.domain.ports.PretradeRiskPort.RejectReason;
import ch.swisstms.domain.ports.PretradeRiskPort.RiskDecision;
import org.springframework.stereotype.Component;

/**
 * FIX Reject(35=3) / BusinessMessageReject(35=j) generator (T247).
 *
 * <p>Maps a {@link RiskDecision.Rejected} from the pre-trade gateway to the proper FIX-level
 * rejection. Session-level violations (malformed messages, sequence-number errors) become {@code
 * 35=3 Reject}; business-level (limit exceeded, unknown client) become {@code 35=j
 * BusinessMessageReject}. The text on the wire is bounded to the FIX 58/Text limit.
 */
@Component
public class FixRejectGenerator {

  /** Reject(35=3) for session-layer violations. */
  public String sessionReject(int refSeqNum, int refTagId, String reason) {
    StringBuilder sb = new StringBuilder();
    appendField(sb, 35, "3");
    appendField(sb, 45, Integer.toString(refSeqNum));
    if (refTagId > 0) {
      appendField(sb, 371, Integer.toString(refTagId));
    }
    appendField(sb, 58, truncate(reason, 256));
    return sb.toString();
  }

  /** BusinessMessageReject(35=j) for higher-layer rejections. */
  public String businessMessageReject(String refMsgType, RiskDecision.Rejected rejected) {
    StringBuilder sb = new StringBuilder();
    appendField(sb, 35, "j");
    appendField(sb, 372, refMsgType);
    appendField(sb, 380, Integer.toString(businessRejectReasonFor(rejected.reason())));
    appendField(sb, 58, truncate(rejected.detail(), 256));
    appendField(sb, 1382, rejected.reason().name()); // RejReasonText (custom)
    return sb.toString();
  }

  /** Map a {@link RejectReason} to a FIX BusinessRejectReason (Tag 380). */
  static int businessRejectReasonFor(RejectReason reason) {
    return switch (reason) {
      case UNKNOWN_CLIENT, UNKNOWN_INSTRUMENT -> 1; // Unknown ID
      case FAT_FINGER_QUANTITY, FAT_FINGER_NOTIONAL, MAX_ORDER_SIZE -> 18; // Invalid Price/Qty
      case KILL_SWITCH_TRIPPED -> 6; // Application not available
      case THROTTLE_PER_SECOND, THROTTLE_IN_FLIGHT -> 5; // Conditionally required missing
      case INSTRUMENT_RESTRICTED -> 4; // Application not available for instrument
      case DAILY_NOTIONAL_LIMIT -> 18;
    };
  }

  private static void appendField(StringBuilder sb, int tag, String value) {
    sb.append(tag).append('=').append(value).append('\u0001');
  }

  private static String truncate(String s, int max) {
    if (s == null) {
      return "";
    }
    return s.length() <= max ? s : s.substring(0, max);
  }
}
