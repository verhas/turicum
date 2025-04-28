package ch.turic.cli;

import ch.turic.Interpreter;
import ch.turic.analyzer.Input;
import ch.turic.commands.operators.Cast;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class Main {

    final static Set<String> parameters = Set.of(
            // snippet command_options
            "version",
            "APPIA",
            "debug",
            "dry",
            "help"
            // end snippet
    );

    public static void main(String[] args) throws IOException {
        final var params = CmdParser.parse(args, parameters);
        if (params.get("version").isPresent()) {
            System.out.printf("Jamal Version %s", "1.0.0-SNAPSHOT");
            return;
        }
        if (params.get("help").isPresent()) {
            System.out.println("Usage: turi [options] program [arguments]\n" +
                    // snippet command_options_help
                    "  -help                      help\n" +
                    "  -version                   display version\n" +
                    "  -debug=<debug>             type:port, http:8080 by default when the value is skipped\n" +
                    "  -dry                       compile only\n" +
                    // end snippet
                    "");
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
            final var interpreter = new Interpreter(Input.fromFile(Path.of(inputFile)));
            final var returnValue = interpreter.execute();
            if (returnValue != null && Cast.isLong(returnValue)) {
                System.exit(Cast.toLong(returnValue).intValue());
            }
        }catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}





