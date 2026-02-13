package ch.turic;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

public class TestReferenceSingleScript {

    private static final Path SNIPPETS_DIR = Path.of("./src/test/resources/references");

    /**
     * Runs exactly one reference script, selected via a JVM system property.
     * <p>
     * Usage (from the repo root):
     * mvn -pl core -Dtest=TestReferenceSingleScript -Dturi.ref=len test
     * <p>
     * Where turi.ref is either:
     * - snippet name (e.g. "len")
     * - or file name (e.g. "len.turi")
     */
    @Test
    void runOneReferenceScriptFromProperty() throws Exception {
        final var raw = System.getProperty("turi.ref");
        final var file = getPath(raw);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Reference script not found: " + file.toAbsolutePath());
        }

        System.out.println("Running reference script: " + file.toAbsolutePath());
        System.out.println("-".repeat(80));
        System.out.println(Files.readString(file));
        System.out.println("-".repeat(80));
        // Intentionally do NOT redirect System.out: this is a dev helper test.
        final Object result;
        try (final var interpreter = new Interpreter(Input.fromFile(file))) {
            result = interpreter.compileAndExecute();
        }
        System.out.println("-".repeat(80));
        System.out.println("Result: " + result);
    }

    /**
     * Resolves and validates the path of a Turicum reference script based on the provided raw input.
     *
     * @param raw the input string representing the script name or file name.
     *            If the input does not end with ".turi", it will be appended automatically.
     *            Must not be null or blank.
     * @return the normalized and resolved {@link Path} object pointing to the script file within the expected directory.
     * @throws IllegalArgumentException if the input is null, blank, resolves to a path outside the expected directory,
     *                                  or if the necessary system property 'turi.ref' is missing or invalid.
     */
    private static Path getPath(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("""
                    Missing system property 'turi.ref'.
                    
                    Example:
                      mvn -pl core -Dtest=TestReferenceSingleScript -Dturi.ref=len test
                    """);
        }

        final var fileName = raw.endsWith(".turi") ? raw : raw + ".turi";
        final var file = SNIPPETS_DIR.resolve(fileName).normalize();

        if (!file.startsWith(SNIPPETS_DIR.normalize())) {
            throw new IllegalArgumentException("Invalid value for system property '-Dturi.ref=" + raw + "'");
        }
        return file;
    }
}