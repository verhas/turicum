package ch.turic;

import ch.turic.analyzer.Input;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class TestReferenceSnippets {
    /**
     * Generates a stream of dynamic tests for program snippets defined in a resource file.
     * Each dynamic test corresponds to a single program snippet, which is compiled and executed
     * using a custom interpreter. The test verifies the snippet's execution behavior and captures
     * the output and execution result for validation or debugging purposes.
     *
     * @return a stream of {@code DynamicTest} instances representing the tests for each program snippet
     * @throws IOException if reading the resource file fails
     */
    @TestFactory
    Stream<DynamicTest> dynamicTestsForInterpreterPrograms() throws IOException {
        // Locate the resource file.
        Path filePath = Path.of("./src/test/resources/references.turi");
        final var outputDir = Path.of("src/test/resources/references_output");
        String absoluteFilePath = filePath.toAbsolutePath().toString();
        // Read all lines from the resource file.
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        // Parse the file content into individual snippets, capturing location info.
        List<ProgramSnippet> snippets = parseSnippets(lines);
        final var out = System.out;
        // Create a dynamic test for each snippet.
        return snippets.stream().map(snippet ->
                DynamicTest.dynamicTest(
                        snippet.name() + " " + snippet.name() + ":" + snippet.lineNumber(),
                        () -> {
                            try (final var baos = new ByteArrayOutputStream();
                                 final var ps = new PrintStream(baos)) {
                                System.setOut(ps);
                                // Execute the snippet.
                                Interpreter interpreter = new Interpreter(new Input(new StringBuilder(snippet.programCode()), snippet.filePath));

                                final var program = interpreter.compile();
                                var result = interpreter.execute(program);
                                ps.flush();
                                baos.flush();

                                var output = outputDir.resolve(snippet.name() + ".txt");
                                final var originalOutput = baos.toString();
                                Files.writeString(output, originalOutput, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);

                                var routput = outputDir.resolve(snippet.name() + "_result.txt");
                                Files.writeString(routput, "" + result, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                            } catch (ExecutionException e) {
                                final var oldSt = e.getStackTrace();
                                if (oldSt != null && oldSt.length > 1) {
                                    final var newSt = new StackTraceElement[oldSt.length + 1];
                                    newSt[0] = new StackTraceElement(snippet.name(), "-", "references.turi", snippet.lineNumber());
                                    for (int i = 0; i < oldSt.length; i++) {
                                        final var st = oldSt[i];
                                        newSt[i + 1] = new StackTraceElement(st.getClassName(), st.getMethodName(), "references.turi", st.getLineNumber() + snippet.lineNumber());
                                    }
                                    e.setStackTrace(newSt);
                                }
                                throw e;
                            }
                        }
                )
        );
    }

    /**
     * Parses the list of file lines into individual program snippets.
     * It also captures the file path and starting line number for each snippet.
     * <p>
     * Expected file format:
     * // snippet <snippetName>
     * // <expected class>
     * // "<expected value>"
     * <multi-line program code>
     */
    private List<ProgramSnippet> parseSnippets(List<String> lines) {
        final var snippets = new ArrayList<ProgramSnippet>();
        final var snippetNames = new HashSet<>();
        int i = 0;
        String snippetName;
        int startLine;
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            if (line.startsWith("// snippet")) {
                snippetName = line.substring("// snippet".length()).trim();
                if (snippetNames.contains(snippetName)) {
                    throw new RuntimeException("Duplicate snippet name: " + snippetName + " line:" + (1 + i));
                }
                snippetNames.add(snippetName);
                i++;
                startLine = i;

                // Collect the program code until the next snippet header.
                StringBuilder codeBuilder = new StringBuilder();
                while (i < lines.size() && !lines.get(i).trim().startsWith("// end snippet")) {
                    codeBuilder.append(lines.get(i)).append("\n");
                    i++;
                }
                i++; // skip end snippet
                snippets.add(new ProgramSnippet(snippetName, codeBuilder.toString(), snippetName + ".turi", startLine));
            } else {
                i++;
                if (i >= lines.size()) break; // no more snippets
            }
        }
        return snippets;
    }


    /**
     * Record representing a program snippet.
     */
    private record ProgramSnippet(String name,
                                  String programCode,
                                  String filePath,
                                  int lineNumber) {
    }
}
