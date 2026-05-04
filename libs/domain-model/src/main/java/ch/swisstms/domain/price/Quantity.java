package ch.swisstms.domain.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Tradeable quantity. Always positive; zero is a defined sentinel. */
public final class Quantity implements Comparable<Quantity> {

  public static final int SCALE = 8;
  public static final Quantity ZERO = new Quantity(BigDecimal.ZERO.setScale(SCALE));

  private final BigDecimal value;

  private Quantity(BigDecimal value) {
    this.value = value;
  }

  public static Quantity of(String literal) {
    return of(new BigDecimal(literal));
  }

  public static Quantity of(BigDecimal value) {
    Objects.requireNonNull(value);
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Quantity must be non-negative: " + value);
    }
    return new Quantity(value.setScale(SCALE, RoundingMode.UNNECESSARY));
  }

  public static Quantity of(long whole) {
    return of(BigDecimal.valueOf(whole));
  }

  public Quantity plus(Quantity other) {
    return new Quantity(this.value.add(other.value).setScale(SCALE, RoundingMode.UNNECESSARY));
  }

  public Quantity minus(Quantity other) {
    BigDecimal next = this.value.subtract(other.value);
    if (next.signum() < 0) {
      throw new IllegalArgumentException("Quantity underflow: " + this + " - " + other);
    }
    return new Quantity(next.setScale(SCALE, RoundingMode.UNNECESSARY));
  }

  public boolean isZero() {
    return value.signum() == 0;
  }

  public boolean isPositive() {
    return value.signum() > 0;
  }

  public BigDecimal toBigDecimal() {
    return value;
  }

  public long toUnscaledLong() {
    return value.movePointRight(SCALE).longValueExact();
  }

  @Override
  public int compareTo(Quantity other) {
    return this.value.compareTo(other.value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Quantity q)) return false;
    return this.value.compareTo(q.value) == 0;
  }

  @Override
  public int hashCode() {
    return value.stripTrailingZeros().hashCode();
  }

  @Override
  public String toString() {
    return value.toPlainString();
  }
}
