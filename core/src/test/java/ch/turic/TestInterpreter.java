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

import static org.junit.jupiter.api.Assertions.*;

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
                            // Handle cases where an exception is expected.
                            if ("Exception".equals(snippet.expectedClass())) {
                                assertThrows(Exception.class, () -> new Interpreter(snippet.programCode()).execute(),
                                        "Expected exception for snippet: " + snippet.name());
                            } else {
                                // Execute the snippet.
                                Object result = new Interpreter(snippet.programCode()).execute();
                                // If the expected class is "null", assert that the result is null.
                                if ("null".equals(snippet.expectedClass())) {
                                    assertNull(result, "Expected null result for snippet: " + snippet.name() + ":" + snippet.lineNumber());
                                } else {
                                    // Otherwise, the result should not be null.
                                    assertNotNull(result, "Expected non-null result for snippet: " + snippet.name() + ":" + snippet.lineNumber());
                                    // Verify the class name.
                                    assertEquals(snippet.expectedClass(), result.getClass().getSimpleName(),
                                            "Expected class mismatch in snippet: " + snippet.name() + ":" + snippet.lineNumber());
                                    // Verify the string representation.
                                    assertEquals(snippet.expectedValue(), result.toString(),
                                            "Expected value mismatch in snippet: " + snippet.name() + ":" + snippet.lineNumber());
                                }
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
    private List<ProgramSnippet> parseSnippets(List<String> lines, String filePath) {
        final var snippets = new ArrayList<ProgramSnippet>();
        final var snippetNames = new HashSet<>();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i).trim();
            if (line.startsWith("// snippet")) {
                // Capture the starting line number (1-indexed).
                int startLine = i + 1;
                String snippetName = line.substring("// snippet".length()).trim();
                if (snippetNames.contains(snippetName)) {
                    throw new RuntimeException("Duplicate snippet name: " + snippetName + " line:" + (1 + i));
                }
                snippetNames.add(snippetName);
                i++;

                // Expected result types.
                if (i >= lines.size()) break;
                String classLine = lines.get(i).trim();
                String expectedClass;
                if (classLine.startsWith("//")) {
                    expectedClass = classLine.substring(2).trim();
                } else {
                    throw new RuntimeException("Unexpected snippet line " + (1 + i) + ": " + lines.get(i).trim() + " Needed // class name or // null");
                }
                i++;
                // Expected result value.
                String expectedValue;
                if (expectedClass.equals("null")) {
                    expectedValue = "null";
                } else {
                    if (i >= lines.size()) break;
                    final String valueLine = lines.get(i).trim();
                    if (valueLine.startsWith("//")) {
                        expectedValue = valueLine.substring(2).trim();
                    } else {
                        throw new RuntimeException("Unexpected snippet line " + (1 + i) + ": " + lines.get(i).trim() + " Needed result after //");
                    }
                    if (expectedValue.startsWith("\"") && expectedValue.endsWith("\"")) {
                        expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
                    }
                    i++;
                }

                // Collect the program code until the next snippet header.
                StringBuilder codeBuilder = new StringBuilder();
                while (i < lines.size() && !lines.get(i).trim().startsWith("// snippet")) {
                    codeBuilder.append(lines.get(i)).append("\n");
                    i++;
                }
                snippets.add(new ProgramSnippet(snippetName, expectedClass, expectedValue, codeBuilder.toString(), filePath, startLine));
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
                                  String expectedClass,
                                  String expectedValue,
                                  String programCode,
                                  String filePath,
                                  int lineNumber) {
    }
}
