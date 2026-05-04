package ch.swisstms.domain.marketdata;

import ch.swisstms.domain.instrument.InstrumentId;
import java.util.List;
import java.util.Objects;

public record SubscriptionRequest(
    String subscriberId, List<InstrumentId> instruments, Level level) {
  public SubscriptionRequest {
    Objects.requireNonNull(subscriberId);
    instruments = List.copyOf(instruments);
    Objects.requireNonNull(level);
  }

  public enum Level {
    L1_TOP_OF_BOOK,
    L2_DEPTH,
    L3_FULL_BOOK
  }
}
