package ch.swisstms.region;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoutingRulesLoaderTest {

  private final RoutingRulesLoader loader = new RoutingRulesLoader(null);

  @Test
  void defaultsCoverAllFourRegions() {
    assertThat(loader.regionForInstrument("XSWX", "ZH")).isEqualTo("ZH");
    assertThat(loader.regionForInstrument("XLON", "ZH")).isEqualTo("LD4");
    assertThat(loader.regionForInstrument("XNYS", "ZH")).isEqualTo("NY4");
    assertThat(loader.regionForInstrument("XTKS", "ZH")).isEqualTo("TY3");
  }

  @Test
  void unknownInstrumentFallsBackToProvidedRegion() {
    assertThat(loader.regionForInstrument("XHKG", "ZH")).isEqualTo("ZH");
  }

  @Test
  void unknownClientFallsBackToProvidedRegion() {
    assertThat(loader.regionForClient("UNKNOWN", "LD4")).isEqualTo("LD4");
  }

  @Test
  void marketHoursPresentForCommonAssetClasses() {
    assertThat(loader.hoursForAssetClass("EQUITY_CH").openUtc()).isEqualTo("07:00");
    assertThat(loader.hoursForAssetClass("EQUITY_US").closeUtc()).isEqualTo("20:00");
  }
}
