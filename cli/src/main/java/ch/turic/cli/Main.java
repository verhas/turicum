package ch.turic.cli;

import ch.turic.Input;
import ch.turic.Interpreter;
import ch.turic.commands.operators.Cast;
import ch.turic.utils.Marshaller;
import ch.turic.utils.Unmarshaller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

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

    public static void main(String[] args) throws IOException {
        final var params = CmdParser.parse(args, parameters);
        if (params.get("version").isPresent()) {

            var buildTime = new String(Main.class.getResourceAsStream("/buildtime.txt").readAllBytes(), StandardCharsets.UTF_8);
            String version = Main.class.getPackage().getImplementationVersion();
            if (version == null) {
                version = "DEV-SNAPSHOT";
            }
            System.out.printf("Turicum Version %s (Built: %s)\n", version, buildTime);
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
        try {
            final Interpreter interpreter;
            if (inputFile.endsWith(".turi")) {
                interpreter = new Interpreter((ch.turic.analyzer.Input)Input.fromFile(Path.of(inputFile)));
            } else if (inputFile.endsWith(".turc")) {
                if (params.get("compile").isPresent()) {
                    System.out.println("'.turc' files are already compiled");
                    return;
                }
                final var bytes = Files.readAllBytes(Path.of(inputFile));
                final var unmarshaller = new Unmarshaller();
                final var code = unmarshaller.deserialize(bytes);
                interpreter = new Interpreter(code);
            } else {
                System.out.println("The program file name has to end with '.turi' or '.turc'");
                return;
            }
            if (params.get("compile").isPresent()) {
                final var program = interpreter.compile();
                final var marshaller = new Marshaller();
                final var bytes = marshaller.serialize(program);
                final var outputFile  = inputFile.substring(0, inputFile.length() - 5) + ".turc";
                Files.write(Path.of(outputFile), bytes);
                return;
            }
            final var returnValue = interpreter.compileAndExecute();
            if (returnValue != null && Cast.isLong(returnValue)) {
                System.exit(Cast.toLong(returnValue).intValue());
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}





