package ch.turic;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TestInterpreter {

    @TestFactory
    Stream<DynamicTest> dynamicTestsForInterpreterPrograms() throws URISyntaxException, IOException {
        // Locate the resource file.
        Path filePath = Paths.get(Objects.requireNonNull(getClass().getResource("/programs.txt")).toURI());
        String absoluteFilePath = filePath.toAbsolutePath().toString();
        // Read all lines from the resource file.
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        // Parse the file content into individual snippets, capturing location info.
        List<ProgramSnippet> snippets = parseSnippets(lines, absoluteFilePath);

        // Create a dynamic test for each snippet.
        return snippets.stream().map(snippet ->
                DynamicTest.dynamicTest(
                        snippet.name() + " " + snippet.name() + ":" + snippet.lineNumber(),
                        () -> {
                            if (snippet.err()) {
                                try {
                                    new Interpreter(snippet.programCode()).compileAndExecute();
                                    throw new AssertionError("Syntax error was not detected " + snippet.name());
                                } catch (BadSyntax ignore) {
                                    // this is what we expected
                                } catch(ExecutionException ignore){

                                }

                            } else {
                                try {
                                    new Interpreter(snippet.programCode()).compileAndExecute();
                                } catch (BadSyntax e) {
                                    throw new AssertionError("Syntax error was detected " + snippet.name(),e);
                                }catch(ExecutionException e) {
                                    throw new AssertionError("Execution error was detected " + snippet.name(), e);
                                } catch (Exception e) {
                                    throw new AssertionError("Unknown error was detected " + snippet.name(), e);
                                }
                            }
                        }));
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
    private List<ProgramSnippet> parseSnippets(List<String> lines, String filePath) {
        final var snippets = new ArrayList<ProgramSnippet>();
        final var snippetNames = new HashSet<>();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            if (line.startsWith("// =") || line.startsWith("// !")) {
                // Capture the starting line number (1-indexed).
                int startLine = i + 1;
                final var naked = line.substring("//".length()).trim();
                final var err = naked.charAt(0) == '!';
                final var snippetName = naked.substring(1);
                if (snippetNames.contains(snippetName)) {
                    throw new RuntimeException("Duplicate snippet name: " + snippetName + " line:" + (1 + i));
                }
                snippetNames.add(snippetName);
                i++;

                // Collect the program code until the next snippet header.
                StringBuilder codeBuilder = new StringBuilder();
                while (i < lines.size() && !(lines.get(i).trim().startsWith("// =")||lines.get(i).trim().startsWith("// !"))) {
                    codeBuilder.append(lines.get(i)).append("\n");
                    i++;
                }
                snippets.add(new ProgramSnippet(snippetName, codeBuilder.toString(), err, filePath, startLine));
            } else {
                i++;
            }
        }
        return snippets;
    }

    /**
     * Record representing a program snippet.
     */
    private record ProgramSnippet(String name,
                                  String programCode,
                                  boolean err,
                                  String filePath,
                                  int lineNumber) {
    }
}
