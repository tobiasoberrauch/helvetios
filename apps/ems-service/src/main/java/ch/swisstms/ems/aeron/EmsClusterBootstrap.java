package ch.swisstms.ems.aeron;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aeron Cluster bootstrap — 5-node Raft, quorum = 3 (T255).
 *
 * <p>Conditional on {@code swisstms.ems.cluster.enabled=true}; in dev / unit-test we run a
 * single-node degenerate cluster which is enough to exercise the matching engine. The Phase 14
 * production deployment runs five nodes per region with Raft membership pinned to the StatefulSet
 * ordinal.
 *
 * <p>The actual Aeron Cluster ConsensusModule + ClusteredService wiring is wrapped in this
 * component so the rest of the EMS code is cluster-agnostic.
 */
@Component
@ConditionalOnProperty(value = "swisstms.ems.cluster.enabled", havingValue = "true")
public class EmsClusterBootstrap {

  private static final Logger log = LoggerFactory.getLogger(EmsClusterBootstrap.class);

  private final int nodeId;
  private final String members;
  private final String archiveDir;

  public EmsClusterBootstrap(
      @Value("${swisstms.ems.cluster.nodeId:0}") int nodeId,
      @Value("${swisstms.ems.cluster.members:0,localhost,9001,9002,9003,9004,9005}") String members,
      @Value("${swisstms.ems.cluster.archiveDir:/var/swisstms/aeron/archive}") String archiveDir) {
    this.nodeId = nodeId;
    this.members = members;
    this.archiveDir = archiveDir;
  }

  @PostConstruct
  public void start() {
    log.info(
        "EMS Aeron cluster bootstrap — nodeId={} members='{}' archiveDir={}",
        nodeId,
        members,
        archiveDir);
    // Phase 13 stops at logging; Phase 14 launches MediaDriver + ClusteredServiceContainer.
  }

  @PreDestroy
  public void stop() {
    log.info("EMS Aeron cluster shutting down nodeId={}", nodeId);
  }

  public int nodeId() {
    return nodeId;
  }
}
