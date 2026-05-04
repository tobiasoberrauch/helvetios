package ch.swisstms.inbound.fix;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Per-client throttle (T243 / FR-005d).
 *
 * <p>Two complementary limits per session:
 *
 * <ul>
 *   <li><b>orders / second</b> — token bucket; refilled at the configured rate, capacity equals the
 *       rate so a 1 s burst is allowed.
 *   <li><b>in-flight orders</b> — a simple counter of orders submitted but not yet ack'd or
 *       rejected. Hard cap; new orders bounce with a BusinessMessageReject(35=j).
 * </ul>
 *
 * <p>Both are off-heap-friendly: only longs / ints. Phase 14 substitutes Agrona structures so the
 * throttle is GC-free on the hot path.
 */
@Component
public class PerClientThrottle {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public boolean tryAcquire(String clientId, int ratePerSecond, int maxInflight) {
    Bucket b = buckets.computeIfAbsent(clientId, id -> new Bucket(ratePerSecond, maxInflight));
    return b.tryAcquire(ratePerSecond, maxInflight);
  }

  public void releaseInflight(String clientId) {
    Bucket b = buckets.get(clientId);
    if (b != null) {
      b.releaseInflight();
    }
  }

  public ThrottleSnapshot snapshot(String clientId) {
    Bucket b = buckets.get(clientId);
    if (b == null) {
      return new ThrottleSnapshot(0, 0, 0);
    }
    return new ThrottleSnapshot(b.tokens.get(), b.inflight.get(), b.totalAccepted.get());
  }

  public record ThrottleSnapshot(int availableTokens, int inflight, long totalAccepted) {}

  private static final class Bucket {
    private final AtomicInteger tokens;
    private final AtomicLong lastRefillMicros = new AtomicLong(System.nanoTime() / 1_000);
    private final AtomicInteger inflight = new AtomicInteger();
    private final AtomicLong totalAccepted = new AtomicLong();

    Bucket(int initialTokens, int maxInflight) {
      this.tokens = new AtomicInteger(initialTokens);
    }

    boolean tryAcquire(int ratePerSecond, int maxInflight) {
      refill(ratePerSecond);
      if (tokens.getAndUpdate(t -> t > 0 ? t - 1 : 0) <= 0) {
        return false;
      }
      if (inflight.incrementAndGet() > maxInflight) {
        // Refund the in-flight slot we just took; the order is rejected.
        inflight.decrementAndGet();
        return false;
      }
      totalAccepted.incrementAndGet();
      return true;
    }

    void releaseInflight() {
      int prev = inflight.getAndUpdate(v -> Math.max(0, v - 1));
      if (prev == 0) {
        // Nothing to release — shouldn't happen, but tolerate it.
      }
    }

    private void refill(int ratePerSecond) {
      long nowMicros = System.nanoTime() / 1_000;
      long last = lastRefillMicros.get();
      long elapsedMicros = nowMicros - last;
      if (elapsedMicros < 1_000) {
        return; // refill at most every ms
      }
      if (lastRefillMicros.compareAndSet(last, nowMicros)) {
        long add = (elapsedMicros * ratePerSecond) / 1_000_000;
        if (add > 0) {
          tokens.updateAndGet(t -> (int) Math.min((long) ratePerSecond, t + add));
        }
      }
    }
  }
}
