package ch.swisstms.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * T230 — every repo-relative path mentioned in {@code docs/interview/30min-walkthrough.md} must
 * exist on disk.
 *
 * <p>The interview walkthrough is the elevator pitch; if a referenced file is missing the candidate
 * looks unprepared. We treat broken walkthrough links as a build break.
 */
class WalkthroughLinkValidityTest {

  // Match backtick-quoted paths that look like repo-relative file references:
  //   `apps/oms-service/`
  //   `docs/decisions/0007-ptp-rts25.md`
  //   `.specify/memory/constitution.md`
  // Excludes shell commands and URLs.
  private static final Pattern PATH_REF =
      Pattern.compile(
          "`((?:apps|libs|tools|docs|infra|ops|tests|mocks|contracts|specs|\\.specify)/[^`\\s]+)`");

  private static final Path REPO_ROOT = repoRoot();

  @Test
  void everyRepoRelativeReferenceInWalkthroughExists() throws IOException {
    Path walkthrough = REPO_ROOT.resolve("docs/interview/30min-walkthrough.md");
    assertThat(walkthrough).exists();

    String text = Files.readString(walkthrough);
    Matcher m = PATH_REF.matcher(text);
    List<String> missing = new ArrayList<>();
    int seen = 0;
    while (m.find()) {
      String relative = m.group(1);
      // Skip glob patterns (`apps/venue-adapter-*`) and ellipses (`libs/.../X.java`) — these are
      // narrative shorthand, not literal paths.
      if (relative.contains("*") || relative.contains("...")) {
        continue;
      }
      seen++;
      // Trim trailing slash for directories so Files.exists works either way.
      Path target =
          REPO_ROOT.resolve(
              relative.endsWith("/") ? relative.substring(0, relative.length() - 1) : relative);
      if (!Files.exists(target)) {
        // Try locating the file anywhere under the first segment — the walkthrough may use a
        // shortened path (e.g. `tests/architecture/HexagonalArchitectureTest.java` for the
        // file at `tests/architecture/src/test/.../HexagonalArchitectureTest.java`).
        if (!findByName(
            REPO_ROOT.resolve(firstSegment(relative)), target.getFileName().toString())) {
          missing.add(relative + "  (resolved: " + target + ")");
        }
      }
    }
    assertThat(seen)
        .as("walkthrough must reference at least 5 repo paths to be useful")
        .isGreaterThanOrEqualTo(5);
    assertThat(missing).as("missing references in 30min-walkthrough.md").isEmpty();
  }

  private static String firstSegment(String relative) {
    int slash = relative.indexOf('/');
    return slash < 0 ? relative : relative.substring(0, slash);
  }

  private static boolean findByName(Path searchRoot, String filename) {
    if (!Files.isDirectory(searchRoot)) {
      return false;
    }
    try (var stream = Files.walk(searchRoot)) {
      return stream.anyMatch(p -> p.getFileName().toString().equals(filename));
    } catch (IOException e) {
      return false;
    }
  }

  private static Path repoRoot() {
    Path here = Paths.get("").toAbsolutePath();
    while (here != null && !Files.exists(here.resolve(".specify"))) {
      here = here.getParent();
    }
    if (here == null) {
      throw new IllegalStateException(
          "could not locate repo root from " + Paths.get("").toAbsolutePath());
    }
    return here;
  }
}
