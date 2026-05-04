package ch.swisstms.domain.order;

import java.util.Objects;

/** Client-supplied order identifier (FIX Tag 11). Unique per (clientId, clOrdId). */
public record ClOrdId(String value) {

  public ClOrdId {
    Objects.requireNonNull(value);
    if (value.isEmpty() || value.length() > 32) {
      throw new IllegalArgumentException("ClOrdID length must be 1..32 — was " + value.length());
    }
  }

  @Override
  public String toString() {
    return value;
  }
}
