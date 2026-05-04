package ch.swisstms.venue.six.tri;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * T299 — SIX TRI (Trade Reporting Interface) SFTP submitter.
 *
 * <p>TRI is the SIX trade-repository inbound endpoint for FinfraG Art. 39 daily submissions. The
 * reporting-service builds the TRI-XML; this submitter handles the SFTP upload + SHA-256 checksum
 * so the audit chain can prove the file delivered intact.
 */
@Component
public class TriSftpSubmitter {

  private static final Logger log = LoggerFactory.getLogger(TriSftpSubmitter.class);

  private final String host;
  private final int port;
  private final String user;
  private final String dropDir;

  public TriSftpSubmitter(
      @Value("${swisstms.six.tri.host:tri.six-group.com}") String host,
      @Value("${swisstms.six.tri.port:22}") int port,
      @Value("${swisstms.six.tri.user:swisstms}") String user,
      @Value("${swisstms.six.tri.dropDir:/tri/inbound/}") String dropDir) {
    this.host = host;
    this.port = port;
    this.user = user;
    this.dropDir = dropDir;
  }

  public record SubmissionAck(String filename, long bytes, String sha256) {}

  public SubmissionAck submit(String filename, byte[] payload) {
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
      sftp.put(new ByteArrayInputStream(payload), filename);
      String sha = sha256(payload);
      log.info(
          "TRI submitted {} ({} bytes, sha256[..7]={})",
          filename,
          payload.length,
          sha.substring(0, 7));
      return new SubmissionAck(filename, payload.length, sha);
    } catch (Exception e) {
      throw new IllegalStateException("TRI SFTP submit failed", e);
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
