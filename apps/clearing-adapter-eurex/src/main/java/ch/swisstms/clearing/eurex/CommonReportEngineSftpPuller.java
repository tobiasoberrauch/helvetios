package ch.swisstms.clearing.eurex;

import ch.swisstms.audit_chain.AuditEvent.ActorType;
import ch.swisstms.audit_chain.HashChainWriter;
import ch.swisstms.domain.ports.ClearingPort.ClearingReport;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Pulls Eurex Common Report Engine (CRE) files via SFTP each business day (US4 / FR-014).
 *
 * <p>The CRE produces TC540 / TR021 / RPTTC… files in EOD batches. We poll the configured drop
 * directory at {@code 18:30 UTC} (≈ 19:30 CET, after Eurex EOD), download every file we have not
 * yet seen, write a hash-chained {@code AuditEvent} per file (Constitution Principle VI), and
 * expose them via {@link ch.swisstms.domain.ports.ClearingPort#pullDailyReports(LocalDate)}.
 *
 * <p>Phase 14 will swap the password-based JSch auth for cert-based with the keys provided through
 * {@code OpenBao} via {@code QpidJmsConfig}.
 */
@Component
public class CommonReportEngineSftpPuller {

  private static final Logger log = LoggerFactory.getLogger(CommonReportEngineSftpPuller.class);
  private static final DateTimeFormatter FILENAME_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final HashChainWriter audit;
  private final String host;
  private final int port;
  private final String user;
  private final String dropDir;
  private final String knownReports = "TC540,TR021,RPTTC,RPTSC,RPTSP";

  public CommonReportEngineSftpPuller(
      HashChainWriter audit,
      @Value("${swisstms.eurex.cre.host:cre.eurexclearing.com}") String host,
      @Value("${swisstms.eurex.cre.port:22}") int port,
      @Value("${swisstms.eurex.cre.user:swisstms}") String user,
      @Value("${swisstms.eurex.cre.dropDir:/cre/PRODUCTION/}") String dropDir) {
    this.audit = audit;
    this.host = host;
    this.port = port;
    this.user = user;
    this.dropDir = dropDir;
  }

  /** Daily poll at 18:30 UTC (after Eurex EOD batch finishes). */
  @Scheduled(cron = "0 30 18 * * MON-FRI")
  public void dailyPoll() {
    LocalDate today = LocalDate.now();
    log.info("Eurex CRE poll start for valueDate={}", today);
    try {
      List<ClearingReport> reports = fetch(today);
      reports.forEach(
          r ->
              audit.append(
                  ActorType.SERVICE,
                  "clearing-adapter-eurex",
                  "clearing.cre.pulled",
                  "ClearingReport",
                  r.reportName(),
                  ("{\"file\":\""
                          + r.reportName()
                          + "\",\"valueDate\":\""
                          + r.valueDate()
                          + "\",\"sha256\":\""
                          + r.checksum()
                          + "\"}")
                      .getBytes(),
                  null));
      log.info("Eurex CRE poll complete: {} files", reports.size());
    } catch (RuntimeException e) {
      log.error("Eurex CRE poll failed for {}: {}", today, e.getMessage());
      audit.append(
          ActorType.SERVICE,
          "clearing-adapter-eurex",
          "clearing.cre.poll-failed",
          "PollAttempt",
          today.toString(),
          ("{\"valueDate\":\"" + today + "\",\"error\":\"" + e.getMessage() + "\"}").getBytes(),
          null);
    }
  }

  /** On-demand fetch (used by integration tests and {@code pullDailyReports}). */
  public List<ClearingReport> fetch(LocalDate valueDate) {
    List<ClearingReport> out = new ArrayList<>();
    Session session = null;
    ChannelSftp sftp = null;
    String stamp = FILENAME_DATE.format(valueDate);
    try {
      JSch jsch = new JSch();
      session = jsch.getSession(user, host, port);
      session.setConfig("StrictHostKeyChecking", "yes");
      session.connect(15_000);
      sftp = (ChannelSftp) session.openChannel("sftp");
      sftp.connect(15_000);
      sftp.cd(dropDir);

      @SuppressWarnings("unchecked")
      Vector<LsEntry> entries = sftp.ls("*" + stamp + "*");
      for (LsEntry entry : entries) {
        if (entry.getAttrs().isDir()) {
          continue;
        }
        String name = entry.getFilename();
        if (knownReports.indexOf(reportPrefix(name)) < 0) {
          continue;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(8 * 1024);
        sftp.get(name, baos);
        byte[] payload = baos.toByteArray();
        out.add(new ClearingReport(name, valueDate, payload, contentType(name), sha256(payload)));
      }
    } catch (JSchException | SftpException e) {
      throw new IllegalStateException("CRE SFTP fetch failed", e);
    } finally {
      if (sftp != null) {
        sftp.disconnect();
      }
      if (session != null) {
        session.disconnect();
      }
    }
    return out;
  }

  private static String reportPrefix(String name) {
    int dash = name.indexOf('_');
    return dash < 0 ? name : name.substring(0, dash);
  }

  private static String contentType(String name) {
    return name.endsWith(".csv")
        ? "text/csv"
        : name.endsWith(".xml") ? "application/xml" : "application/octet-stream";
  }

  private static String sha256(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 must be available", e);
    }
  }
}
