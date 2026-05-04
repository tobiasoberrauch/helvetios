package ch.swisstms.ems.care;

import ch.swisstms.domain.order.Order;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Care-order queue (T253).
 *
 * <p>"Care" orders are orders the trader handles manually — typically because the size or
 * complexity exceeds what the SOR / algo wheel should run on its own. They land in a per-trader
 * queue and stay there until a human claims, executes, or cancels them.
 *
 * <p>The trader UI ({@code apps/trader-ui/src/views/CareOrdersView.tsx}) talks to this queue via
 * the OMS REST API; here we keep the in-memory state. Phase 14 swaps in a Postgres-backed
 * implementation so cross-region trader takeover works.
 */
@Component
public class CareOrderQueue {

  private static final Logger log = LoggerFactory.getLogger(CareOrderQueue.class);

  public record CareEntry(
      String entryId, Order order, String trader, Instant queuedAt, Status status, String notes) {

    public CareEntry withStatus(Status newStatus, String newNotes) {
      return new CareEntry(entryId, order, trader, queuedAt, newStatus, newNotes);
    }
  }

  public enum Status {
    PENDING,
    CLAIMED,
    EXECUTING,
    EXECUTED,
    CANCELLED
  }

  private final ConcurrentMap<String, CareEntry> entries = new ConcurrentHashMap<>();

  public CareEntry enqueue(Order order, String trader) {
    String id = "CARE-" + UUID.randomUUID();
    CareEntry entry = new CareEntry(id, order, trader, Instant.now(), Status.PENDING, null);
    entries.put(id, entry);
    log.info("Care order enqueued {} for trader={}", id, trader);
    return entry;
  }

  public CareEntry claim(String entryId, String trader) {
    CareEntry result =
        entries.computeIfPresent(
            entryId,
            (k, v) ->
                v.status() == Status.PENDING
                    ? v.withStatus(Status.CLAIMED, "claimed by " + trader)
                    : v);
    if (result == null) {
      throw new IllegalArgumentException("Care entry " + entryId + " not found");
    }
    return result;
  }

  public CareEntry mark(String entryId, Status status, String notes) {
    return entries.computeIfPresent(entryId, (k, v) -> v.withStatus(status, notes));
  }

  public List<CareEntry> queueFor(String trader) {
    List<CareEntry> out = new ArrayList<>();
    for (CareEntry e : entries.values()) {
      if (trader.equals(e.trader())) {
        out.add(e);
      }
    }
    out.sort((a, b) -> a.queuedAt().compareTo(b.queuedAt()));
    return out;
  }

  public Map<String, CareEntry> snapshot() {
    return Map.copyOf(entries);
  }
}
