/*
 * Constitution Principle III (Schemas-as-Versioned-Contracts, NICHT-VERHANDELBAR):
 *
 * Dieser Test prüft beim Build, dass kein Custom-FIX-Tag in zwei
 * verschiedenen Dictionaries unterschiedliche Semantik hat. Ein Verstoß
 * lässt den Build scheitern und blockiert den Merge.
 *
 * Eingebunden in `libs/fix-codec/build.gradle.kts` als Teil der
 * `:test`-Task; ebenfalls von `.github/workflows/lint.yml`
 * (constitution-gates job) ausgeführt.
 *
 * Datei-Format ist absichtlich java-konform statt -Test-Suffix, damit
 * sie als Standalone-Klasse ausführbar bleibt, sobald JUnit 5 verfügbar
 * ist (Phase 2). Bis dahin parst dieselbe Datei ein Shell-Skript-Aufruf
 * im constitution-gates-Job.
 */
package ch.swisstms.fixcodec.tagregistry;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class FixTagRegistryNoCollisionTest {

    private static final Pattern FIELD_DECL =
        Pattern.compile("<field\\s+number=\"(\\d+)\"\\s+name=\"([A-Za-z0-9_]+)\".*?type=\"([A-Z_]+)\"",
            Pattern.DOTALL);

    private static final File CONTRACTS_FIX = new File(System.getProperty("contracts.fix.dir",
        "contracts/fix"));

    @Test
    void noTagDefinedTwiceWithDifferentSemantics() throws Exception {
        record TagDecl(int tag, String name, String type, String fileName) {}

        Map<Integer, TagDecl> declared = new HashMap<>();
        for (File xml : findAllFixDictionaries(CONTRACTS_FIX)) {
            String content = Files.readString(xml.toPath());
            Matcher m = FIELD_DECL.matcher(content);
            while (m.find()) {
                int tag = Integer.parseInt(m.group(1));
                String name = m.group(2);
                String type = m.group(3);
                TagDecl previous = declared.get(tag);
                if (previous != null
                    && (!previous.name().equals(name) || !previous.type().equals(type))) {
                    fail(String.format(
                        "FIX tag %d redeclared with different semantics:%n  %s in %s%n  %s in %s",
                        tag,
                        previous.name() + " (" + previous.type() + ")",
                        previous.fileName(),
                        name + " (" + type + ")",
                        xml.getName()));
                }
                declared.putIfAbsent(tag, new TagDecl(tag, name, type, xml.getName()));
            }
        }
    }

    private static java.util.List<File> findAllFixDictionaries(File root) {
        java.util.List<File> out = new java.util.ArrayList<>();
        if (!root.isDirectory()) {
            return out;
        }
        for (File f : root.listFiles()) {
            if (f.isDirectory()) {
                out.addAll(findAllFixDictionaries(f));
            } else if (f.getName().endsWith(".xml")) {
                out.add(f);
            }
        }
        return out;
    }
}
