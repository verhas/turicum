package ch.turic;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

public class ReferenceSnippetFilesGenerator {

    private static final Path REFERENCES_TURI = Path.of("./src/test/resources/references.turi");
    private static final Path SNIPPETS_DIR = Path.of("./src/test/resources/references");

    @Disabled("One-off generator: run manually to (re)generate src/test/resources/references/*.turi, then commit.")
    @Test
    void generateSnippetFilesFromReferencesTuri() throws IOException {
        Files.createDirectories(SNIPPETS_DIR);

        final var lines = Files.readAllLines(REFERENCES_TURI, StandardCharsets.UTF_8);
        final var snippetNames = new HashSet<String>();

        int i = 0;
        while (i < lines.size()) {
            final var line = lines.get(i).trim();
            if (!line.startsWith("// snippet")) {
                i++;
                continue;
            }

            final var snippetName = line.substring("// snippet".length()).trim();
            if (!snippetNames.add(snippetName)) {
                throw new IllegalStateException("Duplicate snippet name: " + snippetName + " line:" + (1 + i));
            }

            i++; // move to the first code line
            final int startLineInReferences = i + 1; // 1-based for humans

            final var code = new StringBuilder();
            //code.append("// from references.turi:").append(startLineInReferences).append("\n");

            while (i < lines.size() && !lines.get(i).trim().startsWith("// end snippet")) {
                code.append(lines.get(i)).append("\n");
                i++;
            }

            if (i >= lines.size()) {
                throw new IllegalStateException("Missing // end snippet for " + snippetName);
            }

            i++; // skip end snippet

            final var outFile = SNIPPETS_DIR.resolve(snippetName + ".turi");
            Files.writeString(outFile, code.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
}