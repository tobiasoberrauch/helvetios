package ch.swisstms.marketdata.storage;

import ch.swisstms.domain.marketdata.MarketDataTick;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * ClickHouse tick-warm writer (T162).
 *
 * <p>Bulk insert into {@code md_l1_warm} (MergeTree, partition by date, order by (mic, isin,
 * biz_time)). Buffer flushes either when 10k rows are queued or every second; ClickHouse loves
 * batches and hates row-at-a-time inserts.
 *
 * <p>Phase 8 keeps the buffer in-process; Phase 14 introduces a dedicated batch service per region.
 */
@Component
@ConditionalOnProperty(value = "swisstms.marketdata.clickhouse.enabled", havingValue = "true")
public class ClickHouseTickWriter {

  private static final Logger log = LoggerFactory.getLogger(ClickHouseTickWriter.class);
  private static final int BATCH_SIZE = 10_000;
  private static final String INSERT =
      "INSERT INTO md_l1_warm (biz_time, isin, mic, bid_px, bid_qty, ask_px, ask_qty, src, seq) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private final String url;
  private final List<MarketDataTick> buffer = new ArrayList<>(BATCH_SIZE);
  private final AtomicLong flushed = new AtomicLong();

  public ClickHouseTickWriter(
      @Value("${swisstms.marketdata.clickhouse.url:jdbc:clickhouse://localhost:8123/swisstms}")
          String url) {
    this.url = url;
  }

  public synchronized void enqueue(MarketDataTick tick) {
    buffer.add(tick);
    if (buffer.size() >= BATCH_SIZE) {
      flush();
    }
  }

  /** Flush every second so warm-tier latency stays bounded even at low-volume. */
  @Scheduled(fixedDelay = 1_000)
  public synchronized void flush() {
    if (buffer.isEmpty()) {
      return;
    }
    try (Connection c = DriverManager.getConnection(url);
        PreparedStatement ps = c.prepareStatement(INSERT)) {
      for (MarketDataTick tick : buffer) {
        ps.setTimestamp(1, Timestamp.from(tick.bizTime()));
        ps.setString(2, tick.instrument().isin());
        ps.setString(3, tick.instrument().mic());
        ps.setBigDecimal(4, tick.bidPrice().toBigDecimal());
        ps.setBigDecimal(5, tick.bidQty().toBigDecimal());
        ps.setBigDecimal(6, tick.askPrice().toBigDecimal());
        ps.setBigDecimal(7, tick.askQty().toBigDecimal());
        ps.setString(8, tick.source());
        ps.setLong(9, tick.sequenceNumber());
        ps.addBatch();
      }
      int[] counts = ps.executeBatch();
      flushed.addAndGet(counts.length);
      log.debug("ClickHouse flushed {} ticks", counts.length);
      buffer.clear();
    } catch (SQLException e) {
      log.error("ClickHouse batch insert failed (size={}): {}", buffer.size(), e.getMessage());
    }
  }

  public long flushedCount() {
    return flushed.get();
  }
}
