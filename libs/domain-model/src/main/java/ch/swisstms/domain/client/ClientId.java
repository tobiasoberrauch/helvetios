package ch.swisstms.domain.client;

import java.util.Objects;
import java.util.UUID;

public record ClientId(UUID value) {

  public ClientId {
    Objects.requireNonNull(value);
  }

  public static ClientId of(String literal) {
    return new ClientId(UUID.fromString(literal));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
