package ch.swisstms.region;

import ch.swisstms.domain.common.Region;
import ch.swisstms.domain.instrument.InstrumentId;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Tags inbound orders with the responsible region. Per-instrument / per-asset-class lookup; default
 * uses follow-the-sun current-region.
 */
@Component
public class RegionTagger {

  private final Region defaultRegion;

  private static final Map<String, Region> MIC_TO_PRIMARY_REGION =
      Map.of(
          "XSWX", Region.ZH,
          "XEUR", Region.LD4,
          "XLON", Region.LD4,
          "XNYS", Region.NY4,
          "XNAS", Region.NY4,
          "XTKS", Region.TY3,
          "XOSE", Region.TY3);

  public RegionTagger(@Value("${swisstms.region:ZH}") String defaultRegion) {
    this.defaultRegion = Region.valueOf(defaultRegion);
  }

  public Region tag(InstrumentId instrument) {
    Region byMic = MIC_TO_PRIMARY_REGION.get(instrument.mic());
    if (byMic != null) return byMic;
    return followTheSunCurrentRegion();
  }

  public Region followTheSunCurrentRegion() {
    int hour = LocalTime.now(ZoneOffset.UTC).getHour();
    if (hour >= 6 && hour < 14) return Region.LD4;
    if (hour >= 14 && hour < 22) return Region.NY4;
    return Region.TY3; // 22:00–06:00 UTC
  }

  public Region defaultRegion() {
    return defaultRegion;
  }
}
