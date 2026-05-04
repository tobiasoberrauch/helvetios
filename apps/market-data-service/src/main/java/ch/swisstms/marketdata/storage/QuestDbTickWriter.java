package ch.swisstms.marketdata.storage;

import ch.swisstms.domain.marketdata.MarketDataTick;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * QuestDB tick-hot writer (T161).
 *
 * <p>Writes ticks to a wide table {@code md_l1} via the Postgres-wire protocol. QuestDB targets
 * sub-millisecond OHLCV reads (≈ 25 ms vs kdb+ 109 ms in the public benchmark). One row per tick;
 * partitioned by day.
 *
 * <p>Disabled by default — flip with {@code swisstms.marketdata.questdb.enabled=true}. Phase 8
 * smoke-tests bind this to a docker QuestDB; Phase 14 runs it on dedicated NVMe.
 */
@Component
@ConditionalOnProperty(value = "swisstms.marketdata.questdb.enabled", havingValue = "true")
public class QuestDbTickWriter {

  private static final Logger log = LoggerFactory.getLogger(QuestDbTickWriter.class);
  private static final String INSERT =
      "INSERT INTO md_l1 (biz_time, isin, mic, bid_px, bid_qty, ask_px, ask_qty, src, seq) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private final String url;
  private final String user;
  private final String password;
  private final AtomicLong written = new AtomicLong();

  public QuestDbTickWriter(
      @Value("${swisstms.marketdata.questdb.url:jdbc:postgresql://localhost:8812/qdb}") String url,
      @Value("${swisstms.marketdata.questdb.user:admin}") String user,
      @Value("${swisstms.marketdata.questdb.password:quest}") String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public void write(MarketDataTick tick) {
    try (Connection c = DriverManager.getConnection(url, user, password);
        PreparedStatement ps = c.prepareStatement(INSERT)) {
      ps.setTimestamp(1, Timestamp.from(tick.bizTime()));
      ps.setString(2, tick.instrument().isin());
      ps.setString(3, tick.instrument().mic());
      ps.setBigDecimal(4, tick.bidPrice().toBigDecimal());
      ps.setBigDecimal(5, tick.bidQty().toBigDecimal());
      ps.setBigDecimal(6, tick.askPrice().toBigDecimal());
      ps.setBigDecimal(7, tick.askQty().toBigDecimal());
      ps.setString(8, tick.source());
      ps.setLong(9, tick.sequenceNumber());
      ps.executeUpdate();
      written.incrementAndGet();
    } catch (SQLException e) {
      log.error("QuestDB tick write failed seq={}: {}", tick.sequenceNumber(), e.getMessage());
    }
  }

  public long writtenCount() {
    return written.get();
  }
}
