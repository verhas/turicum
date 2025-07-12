package ch.turic;

import ch.turic.analyzer.Input;
import ch.turic.utils.Marshaller;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

public class TestCompilation {

    /**
     * Generates a stream of dynamic tests based on program snippets defined in an input file.
     * Each snippet is compiled, executed, and compared for consistency between its original
     * and serialized versions. Both the output and execution results are verified to ensure
     * correctness and consistency under different conditions.
     * <p>
     * The test dynamically assesses various program scenarios to validate the interpreter logic,
     * including handling edge cases and verifying outputs for specific snippet names.
     *
     * @return A stream of dynamic tests, each representing a specific program snippet's
     * compilation, execution, and verification process.
     * @throws URISyntaxException If the provided file path has an invalid syntax.
     * @throws IOException        If there is an error in reading or writing files during processing.
     */
    @TestFactory
    Stream<DynamicTest> dynamicTestsForInterpreterPrograms() throws URISyntaxException, IOException {
        // Locate the resource file.
        Path filePath = Path.of("./src/test/resources/references.turi");
        final var outputDir = Path.of("src/test/resources/references_output");
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
                            try {
                                var baos = new ByteArrayOutputStream();
                                var ps = new PrintStream(baos);
                                System.setOut(ps);
                                // Execute the snippet.
                                Interpreter interpreter = new Interpreter(new Input(new StringBuilder(snippet.programCode()), snippet.filePath));
                                final var program = interpreter.compile();
                                var result = interpreter.execute(program);
                                final var marshaller = new Marshaller();
                                Path turcFile = outputDir.resolve(snippet.name() + ".turc");
                                Files.write(turcFile, marshaller.serialize(program));
                                ps.close();
                                baos.close();
                                var output = outputDir.resolve(snippet.name() + ".txt");
                                final var originalOutput = baos.toString();
                                Files.writeString(output, originalOutput, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                                var routput = outputDir.resolve(snippet.name() + "_result.txt");
                                Files.writeString(routput, "" + result, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);


                                // execute the binary version
                                baos = new ByteArrayOutputStream();
                                ps = new PrintStream(baos);
                                System.setOut(ps);
                                // Execute the snippet.
                                interpreter = new Interpreter(turcFile);
                                result = interpreter.execute(program);
                                ps.close();
                                baos.close();
                                output = outputDir.resolve(snippet.name() + ".turc.txt");
                                String fromCompressedOutput = baos.toString();
                                Files.writeString(output, fromCompressedOutput, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                                routput = outputDir.resolve(snippet.name() + "_result.turc.txt");
                                Files.writeString(routput, "" + result, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
                                System.setOut(out);
                                switch (snippet.name()) {
                                    case "que":
                                    case "try_yield":
                                    case "channel_limit":
                                    case "asyn_list":
                                        // these are non-deterministic due to the parallel nature
                                    case "nano_time": // prints out the time, eventually different "all the time"
                                    case "example_flow_squareroot": // the last digits may differ in the final result
                                    case "example_flow_timeout": // it may be one ir two executions, and they may not be the same in different runs
                                        // difference was manifesting on debian Raspberry
                                    case "decrementable1":
                                    case "decrementable2":
                                    case "incrementable1":
                                    case "incrementable2": // prints out different hex object references
                                        break;
                                    default:
                                        Assertions.assertEquals(fromCompressedOutput, originalOutput, "difference in %s (references.turi:%s)".formatted(snippet.name(), snippet.lineNumber()));
                                }
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
