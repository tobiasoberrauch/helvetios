package ch.swisstms.entitlements.api;

import ch.swisstms.entitlements.cache.EntitlementCache;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** T168 — REST API for entitlement queries + admin updates. */
@RestController
@RequestMapping("/api/v1/entitlements")
public class EntitlementsController {

  private final EntitlementCache cache;

  public EntitlementsController(EntitlementCache cache) {
    this.cache = cache;
  }

  @GetMapping("/{subjectId}")
  public Map<String, Object> get(@PathVariable String subjectId) {
    Set<String> snapshot = cache.snapshot(subjectId);
    return Map.of(
        "subjectId", subjectId,
        "permissions", snapshot,
        "count", snapshot.size());
  }

  @PostMapping("/{subjectId}")
  public Map<String, Object> put(
      @PathVariable String subjectId, @RequestBody Map<String, Object> body) {
    @SuppressWarnings("unchecked")
    Set<String> permissions =
        Set.copyOf((java.util.List<String>) body.getOrDefault("permissions", java.util.List.of()));
    cache.put(subjectId, permissions);
    return Map.of(
        "subjectId", subjectId,
        "permissions", permissions,
        "status", "UPDATED");
  }
}
