package ch.turic;

import ch.turic.analyzer.Input;
import ch.turic.exceptions.ExecutionException;
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
import java.util.List;
import java.util.stream.Stream;

public class TestReferenceSnippets {

    private static final Path SNIPPETS_DIR = Path.of("./src/test/resources/references");
    private static final Path OUTPUT_DIR = Path.of("src/test/resources/references_output");

    @TestFactory
    List<DynamicTest> dynamicTestsForInterpreterPrograms_fromSnippetFiles() throws IOException {
        try (var paths = Files.list(SNIPPETS_DIR)) {
            return paths
                    .filter(p -> p.getFileName().toString().endsWith(".turi"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .map(file ->
                            DynamicTest.dynamicTest(file.getFileName().toString(), () -> runOneSnippetFile(file)))
                    .toList();
        }
    }

    private static void runOneSnippetFile(Path snippetFile) throws Exception {
        final var snippetName = stripExtension(snippetFile.getFileName().toString(), ".turi");
        final var programCode = Files.readString(snippetFile, StandardCharsets.UTF_8);

        try (final var baos = new ByteArrayOutputStream();
             final var ps = new PrintStream(baos);
             final Interpreter interpreter = new Interpreter(new Input(new StringBuilder(programCode), snippetFile.getFileName().toString()))
        ) {
            System.setOut(ps);

            final var program = interpreter.compile();
            final var result = interpreter.execute(program);

            ps.flush();
            baos.flush();

            Files.createDirectories(OUTPUT_DIR);

            final var output = OUTPUT_DIR.resolve(snippetName + ".txt");
            Files.writeString(output, baos.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

            final var routput = OUTPUT_DIR.resolve(snippetName + "_result.txt");
            Files.writeString(routput, "" + result, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (ExecutionException e) {
            throw e;
        } finally {
            System.setOut(System.out);
        }
    }

    private static String stripExtension(String fileName, String ext) {
        return fileName.endsWith(ext) ? fileName.substring(0, fileName.length() - ext.length()) : fileName;
    }
}
