package ch.swisstms.entitlements.api;

import ch.swisstms.domain.ports.EntitlementPort;
import ch.swisstms.domain.ports.EntitlementPort.KillScope;
import ch.swisstms.domain.ports.EntitlementPort.KillScopeType;
import ch.swisstms.domain.ports.EntitlementPort.KillSwitchState;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/killswitch")
public class KillSwitchController {

  private final EntitlementPort entitlements;

  public KillSwitchController(EntitlementPort entitlements) {
    this.entitlements = entitlements;
  }

  @PostMapping("/{scopeType}/{scopeId}/trip")
  public ResponseEntity<Map<String, String>> trip(
      @PathVariable String scopeType,
      @PathVariable String scopeId,
      @RequestBody Map<String, String> body) {
    KillScope scope = new KillScope(KillScopeType.valueOf(scopeType), scopeId);
    String reason = body.getOrDefault("reason", "no reason provided");
    // TODO: extract userId from JWT — Phase 14 wires Spring Security
    // for OAuth2; Phase 8 uses a header for testing.
    String tripper = body.getOrDefault("tripperUserId", "anonymous");
    entitlements.tripKillSwitch(scope, tripper, reason);
    return ResponseEntity.ok(Map.of("scope", scope.toString(), "state", "TRIPPED"));
  }

  @PostMapping("/{scopeType}/{scopeId}/reset")
  public ResponseEntity<Map<String, String>> reset(
      @PathVariable String scopeType,
      @PathVariable String scopeId,
      @RequestBody Map<String, String> body) {
    KillScope scope = new KillScope(KillScopeType.valueOf(scopeType), scopeId);
    String resetter = body.getOrDefault("resetterUserId", "anonymous");
    try {
      entitlements.resetKillSwitch(scope, resetter).toCompletableFuture().get();
      return ResponseEntity.ok(Map.of("scope", scope.toString(), "state", "RESET"));
    } catch (Exception e) {
      return ResponseEntity.status(403)
          .body(
              Map.of(
                  "error",
                  "4-eyes violation",
                  "detail",
                  e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
    }
  }

  @GetMapping("/{scopeType}/{scopeId}")
  public Map<String, String> get(@PathVariable String scopeType, @PathVariable String scopeId) {
    KillScope scope = new KillScope(KillScopeType.valueOf(scopeType), scopeId);
    KillSwitchState state = entitlements.killSwitchFor(scope);
    return Map.of("scope", scope.toString(), "state", state.name());
  }
}
