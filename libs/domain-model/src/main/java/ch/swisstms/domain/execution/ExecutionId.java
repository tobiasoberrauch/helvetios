package ch.swisstms.domain.execution;

import java.util.Objects;
import java.util.UUID;

public record ExecutionId(UUID value) {
  public ExecutionId {
    Objects.requireNonNull(value);
  }

  public static ExecutionId of(String literal) {
    return new ExecutionId(UUID.fromString(literal));
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
