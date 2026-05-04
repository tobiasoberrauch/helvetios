package ch.swisstms.ems.journal;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicLong;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Chronicle Queue persistent journal (T257).
 *
 * <p>Every order/match event the matching engine processes is appended here for compliance replay.
 * Chronicle Queue gives us nanosecond-grade off-heap journaling — RTS-24 expects every
 * regulatory-relevant event to be replayable for ≥ 5 years.
 *
 * <p>Disabled by default; opt-in via {@code swisstms.ems.journal.enabled=true}. Production
 * deployments mount a dedicated NVMe volume at the configured path.
 */
@Component
@ConditionalOnProperty(value = "swisstms.ems.journal.enabled", havingValue = "true")
public class ChronicleQueueJournal {

  private static final Logger log = LoggerFactory.getLogger(ChronicleQueueJournal.class);

  private final String basePath;
  private ChronicleQueue queue;
  private ThreadLocal<ExcerptAppender> appender;
  private final AtomicLong written = new AtomicLong();

  public ChronicleQueueJournal(
      @Value("${swisstms.ems.journal.path:/var/swisstms/journal/ems}") String basePath) {
    this.basePath = basePath;
  }

  @PostConstruct
  public void start() {
    queue = ChronicleQueue.singleBuilder(basePath).build();
    appender = ThreadLocal.withInitial(queue::createAppender);
    log.info("ChronicleQueueJournal opened at {}", basePath);
  }

  @PreDestroy
  public void stop() {
    if (queue != null) {
      queue.close();
      log.info("ChronicleQueueJournal closed ({} entries written)", written.get());
    }
  }

  public void append(String text) {
    appender.get().writeText(text);
    written.incrementAndGet();
  }

  public long writtenCount() {
    return written.get();
  }
}
