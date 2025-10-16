package ch.turic;

import ch.turic.exceptions.BadSyntax;
import ch.turic.exceptions.ExecutionException;
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

    public static final String PREFIX = "-- TEST ";

    @TestFactory
    Stream<DynamicTest> dynamicTestsForInterpreterPrograms() throws URISyntaxException, IOException {

        Path filePath = Paths.get(Objects.requireNonNull(getClass().getResource("/programs.turi")).toURI());
        String absoluteFilePath = filePath.toAbsolutePath().toString();

        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);

        List<Snippet> snippets = parseSnippets(lines, absoluteFilePath);

        return snippets.stream().map(snippet ->
                DynamicTest.dynamicTest(
                        snippet.name() + ":" + snippet.lineNumber(),
                        () -> {
                            if (snippet.err()) {
                                try {
                                    new Interpreter(snippet.programCode()).compileAndExecute();
                                    throw new AssertionError("Syntax error was not detected " + snippet.name() + ":" + snippet.lineNumber());
                                } catch (BadSyntax ignore) {
                                    // this is what we expected
                                } catch (ExecutionException ignore) {

                                }

                            } else {
                                try {
                                    new Interpreter(snippet.programCode()).compileAndExecute();
                                } catch (BadSyntax e) {
                                    throw new AssertionError("Syntax error was detected " + snippet.name() + ":" + snippet.lineNumber(), e);
                                } catch (ExecutionException e) {
                                    final var oldSt = e.getStackTrace();
                                    if (oldSt != null && oldSt.length > 1) {
                                        final var newSt = new StackTraceElement[oldSt.length + 1];
                                        newSt[0] = new StackTraceElement(snippet.name(),"-","programs.turi", snippet.lineNumber());
                                        for( int i = 0 ; i < oldSt.length; i++ ) {
                                            final var st =  oldSt[i];
                                            newSt[i+1] = new StackTraceElement(st.getClassName(),st.getMethodName(),"programs.turi", st.getLineNumber()+ snippet.lineNumber());
                                        }
                                        e.setStackTrace(newSt);
                                    }
                                    throw e;
                                } catch (Exception e) {
                                    throw new AssertionError("Unknown error was detected " + snippet.name() + ":" + snippet.lineNumber(), e);
                                }
                            }
                        }));
    }

    /**
     * Parses a list of lines from a file to extract program snippets. Each snippet is marked
     * with a specific header (//or // !) in the file, and the method captures its metadata
     * such as the name, line numbers, error flag, and code content.
     *
     * @param lines    the list of lines from the file to parse
     * @param filePath the file path of the source file providing context for the snippets
     * @return a list of parsed {@code ProgramSnippet} objects with their respective metadata
     * @throws RuntimeException if a duplicate snippet name is found in the input
     */
    private List<Snippet> parseSnippets(List<String> lines, String filePath) {
        final var snippets = new ArrayList<Snippet>();
        final var snippetNames = new HashSet<>();
        int i = 0;
        while (i < lines.size()) {
            final var line = lines.get(i);
            final var trimmed = line.trim();
            if (trimmed.startsWith(PREFIX)) {
                // Capture the starting line number (1-indexed).
                int startLine = i + 1;
                final var naked = trimmed.substring(PREFIX.length()).trim();
                final var err = naked.charAt(0) == '!';
                final var snippetName = err ? naked.substring(1) : naked;

                // check that the snippet is not duplicated
                if (snippetNames.contains(snippetName)) {
                    throw new RuntimeException("Duplicate snippet name: " + snippetName + " line:" + (1 + i));
                }
                snippetNames.add(snippetName);
                i++;

                // Collect the program code until the next snippet header.
                StringBuilder codeBuilder = new StringBuilder();
                while (i < lines.size() && !lines.get(i).trim().startsWith(PREFIX)) {
                    codeBuilder.append(lines.get(i)).append("\n");
                    i++;
                }
                snippets.add(new Snippet(snippetName, codeBuilder.toString(), err, filePath, startLine));
            } else {
                i++;
            }
        }
        return snippets;
    }

    /**
     * Record representing a program snippet.
     */
    private record Snippet(String name,
                           String programCode,
                           boolean err,
                           String filePath,
                           int lineNumber) {
    }
}
