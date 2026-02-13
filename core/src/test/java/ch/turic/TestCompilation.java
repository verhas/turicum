package ch.turic;

import ch.turic.analyzer.Input;
import ch.turic.exceptions.ExecutionException;
import ch.turic.utils.Marshaller;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Stream;

public class TestCompilation {

    private static final Path SNIPPETS_DIR = Path.of("./src/test/resources/references");
    private static final Path OUTPUT_DIR = Path.of("target/turc_files");

    @TestFactory
    Stream<DynamicTest> dynamicTestsForInterpreterPrograms() throws IOException {
        Files.createDirectories(SNIPPETS_DIR);
        Files.createDirectories(OUTPUT_DIR);

        final var out = System.out;

        try (var paths = Files.list(SNIPPETS_DIR)) {
            final var snippetFiles = paths
                    .filter(p -> p.getFileName().toString().endsWith(".turi"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            if (snippetFiles.isEmpty()) {
                throw new IllegalStateException("No snippet files found in src/test/resources/references/.");
            }

            return snippetFiles.stream().map(file -> DynamicTest.dynamicTest(
                    file.getFileName().toString(),
                    () -> runOneSnippetFile(file, out)
            ));
        }
    }

    private static void runOneSnippetFile(Path snippetFile, PrintStream originalSystemOut) throws Exception {
        final var snippetName = stripExtension(snippetFile.getFileName().toString(), ".turi");
        final var programCode = Files.readString(snippetFile, StandardCharsets.UTF_8);

        try {
            // ---- execute source version, capture output
            Object result;
            Program program;
            String originalOutput;

            try (final var baos = new ByteArrayOutputStream();
                 final var ps = new PrintStream(baos)) {
                System.setOut(ps);

                try (Interpreter interpreter = new Interpreter(new Input(new StringBuilder(programCode), snippetFile.getFileName().toString()))) {
                    program = interpreter.compile();
                    result = interpreter.execute(program);
                }

                ps.flush();
                baos.flush();
                originalOutput = baos.toString();
            }

            writeOutputFromSourceExecution(snippetName, originalOutput, result);
            // ---- serialize compiled program
            final var marshaller = new Marshaller();
            final var turcFile = OUTPUT_DIR.resolve(snippetName + ".turc");
            Files.write(turcFile, marshaller.serialize(program));


            // ---- execute "binary" version, capture output
            String fromCompressedOutput;
            try (final var baos = new ByteArrayOutputStream();
                 final var ps = new PrintStream(baos)) {
                System.setOut(ps);

                try (final Interpreter interpreter = new Interpreter(turcFile)) {
                    result = interpreter.execute(program);
                }

                ps.flush();
                baos.flush();
                fromCompressedOutput = baos.toString();
            }

            writeOutputFromCompiledExecution(snippetName, fromCompressedOutput, result);

            // ---- compare outputs (except for known nondeterministic snippets)
            if (!programCode.startsWith("// stochastic")) {
                Assertions.assertEquals(
                        fromCompressedOutput,
                        originalOutput,
                        "difference in %s (references.turi:%s)".formatted(snippetName, 1)
                );
            }
        } catch (ExecutionException e) {
            throw e;
        } finally {
            System.setOut(originalSystemOut);
        }
    }

    private static void writeOutputFromCompiledExecution(String snippetName, String fromCompressedOutput, Object result) throws IOException {
        Files.writeString(OUTPUT_DIR.resolve(snippetName + ".turc.txt"), fromCompressedOutput, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        Files.writeString(OUTPUT_DIR.resolve(snippetName + "_result.turc.txt"), "" + result, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private static void writeOutputFromSourceExecution(String snippetName, String originalOutput, Object result) throws IOException {
        Files.writeString(OUTPUT_DIR.resolve(snippetName + ".txt"), originalOutput, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        Files.writeString(OUTPUT_DIR.resolve(snippetName + "_result.txt"), "" + result, StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private static String stripExtension(String fileName, String ext) {
        return fileName.endsWith(ext) ? fileName.substring(0, fileName.length() - ext.length()) : fileName;
    }

    // ... existing code ...
}