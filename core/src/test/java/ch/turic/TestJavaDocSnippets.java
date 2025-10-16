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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestJavaDocSnippets {
    @TestFactory
    Stream<DynamicTest> dynamicTestsForJavaDocSnippets() throws IOException {
        final var filePath = Path.of("./src/main/java/");

        // Find all Java files recursively
        final var javaFiles = Files.walk(filePath)
                .filter(path -> path.toString().endsWith(".java"))
                .toList();

        // Extract all JavaDoc snippets from all files
        List<JavaDocSnippet> snippets = new ArrayList<>();
        for (final var javaFile : javaFiles) {
            snippets.addAll(extractJavaDocSnippets(javaFile));
        }

        final var out = System.out;

        // Create a dynamic test for each snippet
        return snippets.stream().map(snippet ->
                DynamicTest.dynamicTest(
                        snippet.name() + " from " + snippet.name() + ":" + snippet.lineNumber(),
                        () -> {
                            final var baos = new ByteArrayOutputStream();
                            final var ps = new PrintStream(baos);
                            try(final Interpreter interpreter = new Interpreter(new Input(new StringBuilder(snippet.programCode()), snippet.filePath()))){
                                // Execute the snippet as Turicum code
                                System.setOut(ps);
                                final var program = interpreter.compile();
                                interpreter.execute(program);
                            } catch (ExecutionException e) {
                                ps.flush();
                                baos.flush();
                                final var ex = new RuntimeException(baos.toString(), e);
                                final var oldSt = e.getStackTrace();
                                if (oldSt != null && oldSt.length > 1) {
                                    final var newSt = new StackTraceElement[oldSt.length + 1];
                                    newSt[0] = new StackTraceElement(snippet.name(), "-", snippet.filePath(), snippet.lineNumber());
                                    for (int i = 0; i < oldSt.length; i++) {
                                        final var st = oldSt[i];
                                        newSt[i + 1] = new StackTraceElement(st.getClassName(), st.getMethodName(),
                                                snippet.filePath(), st.getLineNumber() + snippet.lineNumber());
                                    }
                                    e.setStackTrace(newSt);
                                }
                                throw ex;
                            } finally {
                                ps.close();
                                baos.close();
                                System.setOut(out);
                            }
                        }
                )
        );
    }

    /**
     * Extracts JavaDoc code snippets from a Java file.
     * Looks for snippets between <pre>{@code // comment and }</pre> tags.
     */
    private List<JavaDocSnippet> extractJavaDocSnippets(final Path javaFile) throws IOException {
        final var lines = Files.readAllLines(javaFile, StandardCharsets.UTF_8);
        List<JavaDocSnippet> snippets = new ArrayList<>();

        String relativePath = javaFile.toString();
        int snippetCounter = 1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Look for the start of a JavaDoc code snippet
            if (line.contains("<pre>{@code")) {
                final var sn = String.valueOf(javaFile.getFileName().getName(javaFile.getFileName().getNameCount() - 1));
                final var snippetName = sn.substring(0, sn.length() - 5);
                int startLine = i + 1;
                final var codeBuilder = new StringBuilder();

                i++; // Move to next line to start collecting code

                // Collect lines until we find }</pre>
                while (i < lines.size()) {
                    String currentLine = lines.get(i);

                    if (currentLine.contains("}</pre>")) {
                        break;
                    }

                    // Remove leading asterisks and spaces from JavaDoc comments
                    String cleanLine = currentLine.replaceFirst("^\\s*\\*?\\s?", "");
                    codeBuilder.append(cleanLine).append("\n");
                    i++;
                }

                String code = codeBuilder.toString().trim();
                if (!code.isEmpty()) {
                    snippets.add(new JavaDocSnippet(snippetName, code, relativePath, startLine));
                    snippetCounter++;
                }
            }
        }

        return snippets;
    }

    /**
     * Record representing a JavaDoc code snippet.
     */
    private record JavaDocSnippet(String name,
                                  String programCode,
                                  String filePath,
                                  int lineNumber) {
    }
}
