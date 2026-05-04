package ch.swisstms.domain.order;

import java.util.Objects;
import java.util.UUID;

/** Internal order identifier — UUIDv7 (time-sortable). */
public record OrderId(UUID value) {

  public OrderId {
    Objects.requireNonNull(value, "value");
  }

  public static OrderId of(String literal) {
    return new OrderId(UUID.fromString(literal));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
