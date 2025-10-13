package ch.turic.cli;

import ch.turic.ExecutionException;
import ch.turic.Input;
import ch.turic.Interpreter;
import ch.turic.commands.operators.Cast;
import ch.turic.utils.Unmarshaller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

/**
 * The {@code Main} class serves as the entry point of the application. It processes command-line
 * arguments, configures program settings, and executes the specified actions, such as displaying
 * version information, starting a REPL environment, or compiling and running the program files.
 * <p>
 * The application supports the following command-line options:
 * - {@code -version}: Displays the current application version and build time.
 * - {@code -help}: Prints the usage information and available options.
 * - {@code -APPIA=<import path>}: Specifies import directories.
 * - {@code -compile}: Compiles the input program file to a `.turc` format.
 * - {@code -REPL}: Launches the REPL (Read-Eval-Print Loop) environment.
 * <p>
 * It expects a file with one of the following extensions for program execution:
 * - {@code .turi}: The source code to be compiled and executed.
 * - {@code .turc}: A pre-compiled bytecode file to be executed.
 * <p>
 * Behavior:
 * - If no valid program file is provided, an {@link IllegalArgumentException} is thrown.
 * - If the file has an unrecognized extension, an error message is displayed.
 * - Compilation is optional and can be triggered with the {@code -compile} flag.
 * <p>
 * Handles unhandled exceptions by printing the stack trace to the standard error stream and
 * exiting with a non-zero status.
 */
public class Main {

    final static Set<String> parameters = Set.of(
            // snippet command_options
            "version",
            "APPIA",
            "compile",
            "REPL",
            "help"
            // end snippet
    );

    /**
     * Entry point for the application. This method interprets command-line
     * arguments and executes the desired functionality, such as displaying
     * version information, starting a REPL session, compiling a program,
     * or running a program file. It handles various options and input files
     * based on the provided arguments.
     *
     * @param args An array of strings containing the command-line arguments
     *             passed to the program. Supported options include:
     *             - "-help": Displays available options and their descriptions.
     *             - "-version": Displays the application version and build time.
     *             - "-APPIA": Specifies import paths for finding files during imports.
     *             - "-compile": Compiles the specified program but does not execute it.
     *             - "-REPL": Launches the REPL (Read-Eval-Print Loop) environment.
     *             Additionally, the last positional argument is the file path to
     *             a program to execute, which must end with ".turi" or ".turc".
     * @throws IOException              If an I/O error occurs during properties file access,
     *                                  compilation, or file reading/writing.
     * @throws IllegalArgumentException If no program file is specified or an
     *                                  invalid command-line argument is provided.
     */
    public static void main(String[] args) throws IOException {
        final var params = CmdParser.parse(args, parameters);
        if (params.get("version").isPresent()) {
            String buildTime = "unknown";
            final var properties = new Properties();
            try (InputStream input = Main.class.getResourceAsStream("/build.properties")) {
                if (input != null) {
                    properties.load(input);
                }
                buildTime = properties.getProperty("build.time", "unknown");
            } catch (IOException ignored) {
            }
            String version = Main.class.getPackage().getImplementationVersion();
            if (version == null) {
                version = "DEV-SNAPSHOT";
            }
            System.out.printf("Turicum Version %s (Built: %s)\n", version, buildTime);
            System.out.println("JVM Version: " + System.getProperty("java.version"));
            System.out.println("JVM Version Details: " + System.getProperty("java.runtime.version"));
            System.out.println("JVM Name: " + System.getProperty("java.vm.name"));
            System.out.println("\n=== Operating System Information ===");
            System.out.println("OS Name: " + System.getProperty("os.name"));
            System.out.println("OS Version: " + System.getProperty("os.version"));
            System.out.println("OS Architecture: " + System.getProperty("os.arch"));
            System.out.println("\n=== Hardware Information ===");
            System.out.println("Available Processors (cores): " + Runtime.getRuntime().availableProcessors());
            System.out.println("Total Memory (bytes): " + Runtime.getRuntime().totalMemory());
            System.out.println("Max Memory (bytes): " + Runtime.getRuntime().maxMemory());
            System.out.println("Free Memory (bytes): " + Runtime.getRuntime().freeMemory());
            System.out.println("\n=== File System Information ===");
            System.out.println("File Separator: " + FileSystems.getDefault().getSeparator());
            System.out.println("User Home Directory: " + System.getProperty("user.home"));
            System.out.println("User Working Directory: " + System.getProperty("user.dir"));

            return;
        }
        if (params.get("help").isPresent()) {
            System.out.println("Usage: turi [options] program [arguments]\n" +
                    // snippet command_options_help
                    "  -help                      help\n" +
                    "  -version                   display version\n" +
                    "  -APPIA=<import path>       list of directories looking for files when importing\n" +
                    "  -compile                   compile only\n" +
                    "  -REPL                      start REPL" +
                    // end snippet
                    "");
            return;
        }
        if (params.get("REPL").isPresent()) {
            JLineRepl.execute();
            return;
        }
        if (params.get(0).isEmpty()) {
            throw new IllegalArgumentException("You must specify a program to run");
        }
        final var inputFile = params.get(0).get();
        if (params.get("APPIA").isPresent()) {
            System.setProperty("APPIA", params.get("APPIA").get());
        }

        try (final Interpreter interpreter = getInterpreter(inputFile, params)) {
            if (params.get("compile").isPresent()) {
                interpreter.compile();
                final var bytes = interpreter.serialize();
                final var outputFile = inputFile.substring(0, inputFile.length() - 5) + ".turc";
                Files.write(Path.of(outputFile), bytes);
                return;
            }
            final var returnValue = interpreter.compileAndExecute();
            if (returnValue != null && Cast.isLong(returnValue)) {
                System.exit(Cast.toLong(returnValue).intValue());
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static Interpreter getInterpreter(final String inputFile, final CmdParser params) throws IOException {
        if (inputFile.endsWith(".turi")) {
            return new Interpreter(Input.fromFile(Path.of(inputFile)));
        } else if (inputFile.endsWith(".turc")) {
            if (params.get("compile").isPresent()) {
                throw new ExecutionException("'.turc' files are already compiled");
            }
            final var bytes = Files.readAllBytes(Path.of(inputFile));
            final var unmarshaller = new Unmarshaller();
            final var code = unmarshaller.deserialize(bytes);
            return new Interpreter(code);
        } else {
            throw new ExecutionException("The program file name has to end with '.turi' or '.turc'");
        }

    }
}





