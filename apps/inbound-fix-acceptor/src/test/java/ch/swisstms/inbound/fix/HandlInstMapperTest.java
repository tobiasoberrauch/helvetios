package ch.swisstms.inbound.fix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.swisstms.domain.order.RoutingMode;
import org.junit.jupiter.api.Test;

class HandlInstMapperTest {

  private final HandlInstMapper m = new HandlInstMapper();

  @Test
  void mapsAllThreeStandardValues() {
    assertThat(m.toRoutingMode('1')).isEqualTo(RoutingMode.DMA);
    assertThat(m.toRoutingMode('2')).isEqualTo(RoutingMode.ALGO_WHEEL);
    assertThat(m.toRoutingMode('3')).isEqualTo(RoutingMode.CARE);
  }

  @Test
  void rejectsUnknownValues() {
    assertThatThrownBy(() -> m.toRoutingMode('9'))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("HandlInst");
  }

  @Test
  void roundTripsCleanly() {
    for (RoutingMode mode : RoutingMode.values()) {
      assertThat(m.toRoutingMode(m.toFixValue(mode))).isEqualTo(mode);
    }
  }
}
