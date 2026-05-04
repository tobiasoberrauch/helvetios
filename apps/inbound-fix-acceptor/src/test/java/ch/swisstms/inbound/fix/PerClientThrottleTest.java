package ch.swisstms.inbound.fix;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PerClientThrottleTest {

  private final PerClientThrottle throttle = new PerClientThrottle();

  @Test
  void firstAcquireSucceeds() {
    assertThat(throttle.tryAcquire("ACME", 100, 10)).isTrue();
  }

  @Test
  void exhaustingTokensCausesRejection() {
    boolean allOk = true;
    for (int i = 0; i < 10; i++) {
      allOk &= throttle.tryAcquire("ACME", 10, 100);
    }
    assertThat(allOk).isTrue();
    // 11th in same millisecond should fail (token bucket exhausted, no refill yet).
    assertThat(throttle.tryAcquire("ACME", 10, 100)).isFalse();
  }

  @Test
  void exceedingInflightCapIsRejected() {
    for (int i = 0; i < 5; i++) {
      assertThat(throttle.tryAcquire("BLB", 1000, 5)).isTrue();
    }
    // 6th order: token-bucket OK, but inflight cap hit.
    assertThat(throttle.tryAcquire("BLB", 1000, 5)).isFalse();
    // Releasing one should let the next one through.
    throttle.releaseInflight("BLB");
    assertThat(throttle.tryAcquire("BLB", 1000, 5)).isTrue();
  }
}
