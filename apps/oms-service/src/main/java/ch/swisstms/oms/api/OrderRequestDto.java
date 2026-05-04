package ch.swisstms.oms.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record OrderRequestDto(
    @NotBlank String clOrdId,
    @NotNull InstrumentDto instrumentId,
    @NotBlank @Pattern(regexp = "BUY|SELL|SELL_SHORT") String side,
    @NotBlank @Pattern(regexp = "MARKET|LIMIT|STOP|STOP_LIMIT|FUNARI|MOO|LOO") String ordType,
    @NotNull @Positive BigDecimal quantity,
    BigDecimal price,
    @NotBlank @Pattern(regexp = "DAY|IOC|FOK|GTC|GTD|OPG") String timeInForce,
    Instant expireTime,
    @NotBlank @Pattern(regexp = "DMA|ALGO_WHEEL|CARE") String routingMode,
    @Pattern(regexp = "VWAP|TWAP|POV|IS") String algoStrategy,
    Map<String, String> algoParameters,
    @Pattern(regexp = "[A-Z0-9]{4}") String preferredVenue) {
  public record InstrumentDto(
      @NotBlank @Pattern(regexp = "[A-Z]{2}[A-Z0-9]{9}[0-9]") String isin,
      @NotBlank @Pattern(regexp = "[A-Z0-9]{4}") String mic) {}
}
