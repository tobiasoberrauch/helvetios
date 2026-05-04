package ch.swisstms.reconciler;

import java.util.Objects;

/**
 * Composite reconciliation key — see Constitution Principle V.
 *
 * <p>Drop-Copy gewinnt bei Disagreements; der Join-Key ist (SenderCompID, ClOrdID, ExecID).
 */
public record JoinKey(String senderCompId, String clOrdId, String execId) {

  public JoinKey {
    Objects.requireNonNull(senderCompId);
    Objects.requireNonNull(clOrdId);
    Objects.requireNonNull(execId);
  }

  public String asString() {
    return senderCompId + "|" + clOrdId + "|" + execId;
  }

  public static JoinKey parse(String s) {
    String[] parts = s.split("\\|", 3);
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid join key: " + s);
    }
    return new JoinKey(parts[0], parts[1], parts[2]);
  }
}
