package ch.swisstms.venue.bloomberg.dl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bloomberg Data License nightly SFTP puller (T180).
 *
 * <p>The Bloomberg DL service drops EOD instrument-master snapshots into the contracted SFTP
 * endpoint; we pick them up at 03:30 UTC, hash them, and hand the bytes to the
 * reference-data-service for ingest into Postgres. Every download is recorded to the audit chain
 * via the upstream caller (the puller itself stays I/O-only so it can be unit-tested with a stub
 * SFTP server).
 */
@Component
public class DataLicensePuller {

  private static final Logger log = LoggerFactory.getLogger(DataLicensePuller.class);

  private final String host;
  private final int port;
  private final String user;
  private final String dropDir;

  public DataLicensePuller(
      @Value("${swisstms.bloomberg.dl.host:dlsftp.bloomberg.com}") String host,
      @Value("${swisstms.bloomberg.dl.port:22}") int port,
      @Value("${swisstms.bloomberg.dl.user:swisstms}") String user,
      @Value("${swisstms.bloomberg.dl.dropDir:/dl/PROD/}") String dropDir) {
    this.host = host;
    this.port = port;
    this.user = user;
    this.dropDir = dropDir;
  }

  public record DataLicenseDownload(String filename, byte[] payload, String sha256) {}

  @Scheduled(cron = "0 30 3 * * *")
  public void nightlyPull() {
    LocalDate today = LocalDate.now();
    log.info("Bloomberg DL nightly pull start for {}", today);
    try {
      DataLicenseDownload dl = fetchInstrumentMaster(today);
      log.info(
          "Bloomberg DL pulled {} ({} bytes, sha256[..7]={})",
          dl.filename(),
          dl.payload().length,
          dl.sha256().substring(0, 7));
    } catch (RuntimeException e) {
      log.error("Bloomberg DL nightly pull failed: {}", e.getMessage());
    }
  }

  /** Fetch a single named file from the Bloomberg DL SFTP drop. */
  public DataLicenseDownload fetchInstrumentMaster(LocalDate valueDate) {
    String name = "INSTRUMENT_MASTER_" + valueDate + ".csv";
    Session session = null;
    ChannelSftp sftp = null;
    try {
      JSch jsch = new JSch();
      session = jsch.getSession(user, host, port);
      session.setConfig("StrictHostKeyChecking", "yes");
      session.connect(15_000);
      sftp = (ChannelSftp) session.openChannel("sftp");
      sftp.connect(15_000);
      sftp.cd(dropDir);
      ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
      sftp.get(name, baos);
      byte[] payload = baos.toByteArray();
      return new DataLicenseDownload(name, payload, sha256(payload));
    } catch (JSchException | SftpException e) {
      throw new IllegalStateException("Bloomberg DL fetch failed", e);
    } finally {
      if (sftp != null) {
        sftp.disconnect();
      }
      if (session != null) {
        session.disconnect();
      }
    }
  }

  private static String sha256(byte[] data) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
