package ch.swisstms.domain.price;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Decimal-tick-aware monetary value. Never use {@code double} for prices.
 *
 * <p>Construct via the factory methods:
 *
 * <pre>{@code
 * Price.of("123.45")     // exact
 * Price.scaled(12345, 2) // 123.45
 * Price.MARKET           // sentinel for market orders (no price)
 * }</pre>
 *
 * <p>The internal representation is a {@link BigDecimal} with a fixed scale of 8 (sufficient for
 * global cash equities, listed derivatives, FX, and fixed-income coupon prices).
 */
public final class Price implements Comparable<Price> {

  public static final int SCALE = 8;
  public static final Price ZERO = new Price(BigDecimal.ZERO.setScale(SCALE));
  public static final Price MARKET = new Price(null); // sentinel

  private final BigDecimal value; // null iff this == MARKET

  private Price(BigDecimal value) {
    this.value = value;
  }

  public static Price of(String literal) {
    Objects.requireNonNull(literal, "literal");
    return new Price(new BigDecimal(literal).setScale(SCALE, RoundingMode.HALF_EVEN));
  }

  public static Price of(BigDecimal value) {
    Objects.requireNonNull(value, "value");
    return new Price(value.setScale(SCALE, RoundingMode.HALF_EVEN));
  }

  public static Price scaled(long unscaled, int scale) {
    return new Price(BigDecimal.valueOf(unscaled, scale).setScale(SCALE, RoundingMode.HALF_EVEN));
  }

  public boolean isMarket() {
    return value == null;
  }

  public BigDecimal toBigDecimal() {
    if (isMarket()) {
      throw new IllegalStateException("MARKET price has no numeric value");
    }
    return value;
  }

  public long toUnscaledLong() {
    if (isMarket()) {
      return Long.MIN_VALUE;
    }
    return value.movePointRight(SCALE).longValueExact();
  }

  @Override
  public int compareTo(Price other) {
    Objects.requireNonNull(other);
    if (this.isMarket() && other.isMarket()) return 0;
    if (this.isMarket()) return 1;
    if (other.isMarket()) return -1;
    return this.value.compareTo(other.value);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Price p)) return false;
    if (this.isMarket() != p.isMarket()) return false;
    if (this.isMarket()) return true;
    return this.value.compareTo(p.value) == 0;
  }

  @Override
  public int hashCode() {
    return isMarket() ? 0 : value.stripTrailingZeros().hashCode();
  }

  @Override
  public String toString() {
    return isMarket() ? "MARKET" : value.toPlainString();
  }
}
