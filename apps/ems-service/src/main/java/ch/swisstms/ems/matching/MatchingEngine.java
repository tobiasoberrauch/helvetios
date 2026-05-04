package ch.swisstms.ems.matching;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Single-writer matching engine on a Disruptor ring buffer (T256).
 *
 * <p>Constitution Principle II — hot tier. Producers (per-venue adapters, OMS routing) publish onto
 * the ring; a single consumer thread processes them in order. The single-writer constraint
 * eliminates lock contention and gives us a stable p99 budget.
 *
 * <p>Phase 13 ships the dispatch loop with a no-op handler so the build is exercised; Phase 14
 * plugs in the actual order-book + match-cross logic.
 */
@Component
public class MatchingEngine {

  private static final Logger log = LoggerFactory.getLogger(MatchingEngine.class);
  private static final int RING_SIZE = 1 << 14; // 16k events

  public static final class MatchEvent {
    public long sequence;
    public String orderId;
    public byte side; // '1'=BUY, '2'=SELL
    public long quantity;
    public long priceCents;
    public byte action; // 'N'=NEW, 'C'=CANCEL, 'M'=MODIFY

    public void clear() {
      sequence = 0;
      orderId = null;
      side = 0;
      quantity = 0;
      priceCents = 0;
      action = 0;
    }
  }

  private final AtomicLong processed = new AtomicLong();
  private final EventFactory<MatchEvent> factory = MatchEvent::new;
  private final EventHandler<MatchEvent> handler =
      (event, sequence, endOfBatch) -> processed.incrementAndGet();
  private Disruptor<MatchEvent> disruptor;
  private RingBuffer<MatchEvent> ring;

  @PostConstruct
  public void start() {
    disruptor =
        new Disruptor<>(
            factory,
            RING_SIZE,
            DaemonThreadFactory.INSTANCE,
            ProducerType.MULTI,
            new com.lmax.disruptor.YieldingWaitStrategy());
    disruptor.handleEventsWith(handler);
    ring = disruptor.start();
    log.info("MatchingEngine Disruptor started (ringSize={})", RING_SIZE);
  }

  @PreDestroy
  public void stop() {
    if (disruptor != null) {
      disruptor.shutdown();
      log.info("MatchingEngine Disruptor shutdown ({} events processed)", processed.get());
    }
  }

  public void publish(String orderId, char side, long qty, long priceCents, char action) {
    long seq = ring.next();
    try {
      MatchEvent ev = ring.get(seq);
      ev.sequence = seq;
      ev.orderId = orderId;
      ev.side = (byte) side;
      ev.quantity = qty;
      ev.priceCents = priceCents;
      ev.action = (byte) action;
    } finally {
      ring.publish(seq);
    }
  }

  public long processedCount() {
    return processed.get();
  }
}
