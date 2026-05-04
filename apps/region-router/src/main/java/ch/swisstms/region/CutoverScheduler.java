package ch.swisstms.region;

import ch.swisstms.domain.common.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Follow-the-sun cutover scheduler.
 *
 * <p>FR-042d — bei den konfigurierten Cutover-Zeiten wird die Region-Verantwortung umgeschwenkt.
 * In-flight Orders werden via Kafka MirrorMaker 2 cross-region replicated; die neue Region nimmt
 * über den OMS-Event-Store die State an.
 *
 * <p>Default-Schedule (UTC):
 *
 * <ul>
 *   <li>06:00 UTC — TY3 → LD4 (Tokyo schließt, London öffnet)
 *   <li>14:00 UTC — LD4 → NY4 (London schließt, NY öffnet)
 *   <li>22:00 UTC — NY4 → TY3 (NY schließt, Tokyo öffnet)
 * </ul>
 */
@Component
public class CutoverScheduler {

  private static final Logger log = LoggerFactory.getLogger(CutoverScheduler.class);
  private static final String TOPIC = "region.handover.cutover.v1";

  private final KafkaTemplate<String, String> kafka;

  public CutoverScheduler(KafkaTemplate<String, String> kafka) {
    this.kafka = kafka;
  }

  @Scheduled(cron = "0 0 6 * * *")
  public void tokyoToLondon() {
    publishCutover(Region.TY3, Region.LD4);
  }

  @Scheduled(cron = "0 0 14 * * *")
  public void londonToNewYork() {
    publishCutover(Region.LD4, Region.NY4);
  }

  @Scheduled(cron = "0 0 22 * * *")
  public void newYorkToTokyo() {
    publishCutover(Region.NY4, Region.TY3);
  }

  private void publishCutover(Region from, Region to) {
    String json =
        String.format(
            "{\"from\":\"%s\",\"to\":\"%s\",\"timestamp\":\"%s\"}",
            from, to, java.time.Instant.now());
    log.info("Region cutover: {} → {}", from, to);
    kafka.send(TOPIC, from.name() + "->" + to.name(), json);
  }
}
