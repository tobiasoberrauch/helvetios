package ch.swisstms.venue.bidfx;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * BidFX subject-builder DSL (T197).
 *
 * <p>BidFX wire format addresses every quote stream through a comma-delimited subject of key=value
 * pairs (LiquidityProvider, AssetClass, Symbol, Tenor, Quantity, Currency, …). The builder is
 * fluent and immutable so RFS subscriptions can be assembled incrementally.
 *
 * <p>Example:
 *
 * <pre>{@code
 * Subject s = subjects.build()
 *     .source("BARX")
 *     .symbol("EUR/USD")
 *     .tenor("SPOT")
 *     .quantity(new BigDecimal("1000000"))
 *     .build();
 * }</pre>
 */
@Component
public class SubjectBuilder {

  public Builder build() {
    return new Builder();
  }

  /** Serialize a subject to BidFX wire format. */
  public String toWireString(Subject s) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> e : s.fields().entrySet()) {
      if (!first) {
        sb.append(',');
      }
      sb.append(e.getKey()).append('=').append(e.getValue());
      first = false;
    }
    return sb.toString();
  }

  public static final class Subject {
    private final LinkedHashMap<String, String> fields;
    private final BigDecimal quantity;

    private Subject(LinkedHashMap<String, String> fields, BigDecimal quantity) {
      this.fields = new LinkedHashMap<>(fields);
      this.quantity = quantity;
    }

    public Map<String, String> fields() {
      return java.util.Collections.unmodifiableMap(fields);
    }

    public BigDecimal quantity() {
      return quantity;
    }
  }

  public static final class Builder {
    private final LinkedHashMap<String, String> fields = new LinkedHashMap<>();
    private BigDecimal quantity = BigDecimal.ONE;

    public Builder source(String lp) {
      fields.put("LiquidityProvider", lp);
      return this;
    }

    public Builder assetClass(String ac) {
      fields.put("AssetClass", ac);
      return this;
    }

    public Builder symbol(String symbol) {
      fields.put("Symbol", symbol);
      return this;
    }

    public Builder tenor(String tenor) {
      fields.put("Tenor", tenor);
      return this;
    }

    public Builder quantity(BigDecimal qty) {
      this.quantity = qty;
      fields.put("Quantity", qty.toPlainString());
      return this;
    }

    public Builder currency(String ccy) {
      fields.put("Currency", ccy);
      return this;
    }

    public Builder field(String key, String value) {
      fields.put(key, value);
      return this;
    }

    public Subject build() {
      return new Subject(fields, quantity);
    }
  }
}
