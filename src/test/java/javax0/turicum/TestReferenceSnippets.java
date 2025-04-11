package javax0.turicum;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TestReferenceSnippets {

    @TestFactory
    Stream<DynamicTest> dynamicTestsForInterpreterPrograms() throws URISyntaxException, IOException {
        // Locate the resource file.
        Path filePath = Paths.get(Objects.requireNonNull(getClass().getResource("/references.turi")).toURI());
        final var outputDir = Path.of("src/test/resources/references_output");
        String absoluteFilePath = filePath.toAbsolutePath().toString();
        // Read all lines from the resource file.
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        // Parse the file content into individual snippets, capturing location info.
        List<ProgramSnippet> snippets = parseSnippets(lines, absoluteFilePath);
        final var out = System.out;
        // Create a dynamic test for each snippet.
        return snippets.stream().map(snippet ->
                DynamicTest.dynamicTest(
                        snippet.name() + " " + snippet.name() + ":" + snippet.lineNumber(),
                        () -> {
                            final var baos = new ByteArrayOutputStream();
                            final var ps = new PrintStream(baos);
                            System.setOut(ps);
                            // Execute the snippet.
                            Object result = new Interpreter(snippet.programCode()).execute();
                            ps.close();
                            baos.close();
                            final var output = outputDir.resolve(snippet.name() + ".txt");
                            Files.writeString(output, baos.toString(), StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                            final var routput = outputDir.resolve(snippet.name() + "_result.txt");
                            Files.writeString(routput, ""+result, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                            System.setOut(out);
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
                snippets.add(new ProgramSnippet(snippetName, codeBuilder.toString(), filePath, startLine));
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
