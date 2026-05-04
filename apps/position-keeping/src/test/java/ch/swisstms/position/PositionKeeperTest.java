package ch.swisstms.position;

import static org.assertj.core.api.Assertions.assertThat;

import ch.swisstms.position.PositionKeeper.PositionKey;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PositionKeeperTest {

  private final PositionKeeper keeper = new PositionKeeper();
  private final PositionKey key = new PositionKey("CL-1", "CH0038863350", "XSWX");

  @Test
  void firstBuyEstablishesPositionAtFillPrice() {
    var pos = keeper.applyFill(key, "BUY", new BigDecimal("100"), new BigDecimal("100.00"));
    assertThat(pos.quantity()).isEqualByComparingTo("100");
    assertThat(pos.averagePriceCcyAmount()).isEqualByComparingTo("100.00");
  }

  @Test
  void secondBuyAveragesPrices() {
    keeper.applyFill(key, "BUY", new BigDecimal("100"), new BigDecimal("100.00"));
    var pos = keeper.applyFill(key, "BUY", new BigDecimal("100"), new BigDecimal("110.00"));
    assertThat(pos.quantity()).isEqualByComparingTo("200");
    assertThat(pos.averagePriceCcyAmount()).isEqualByComparingTo("105.00");
  }

  @Test
  void sellReducesQuantityKeepsAverage() {
    keeper.applyFill(key, "BUY", new BigDecimal("100"), new BigDecimal("100.00"));
    var pos = keeper.applyFill(key, "SELL", new BigDecimal("30"), new BigDecimal("105.00"));
    assertThat(pos.quantity()).isEqualByComparingTo("70");
  }

  @Test
  void sideReversalResetsAverageToLatestPrice() {
    keeper.applyFill(key, "BUY", new BigDecimal("100"), new BigDecimal("100.00"));
    var pos = keeper.applyFill(key, "SELL", new BigDecimal("250"), new BigDecimal("105.00"));
    assertThat(pos.quantity()).isEqualByComparingTo("-150");
    assertThat(pos.averagePriceCcyAmount()).isEqualByComparingTo("105.00");
  }
}
