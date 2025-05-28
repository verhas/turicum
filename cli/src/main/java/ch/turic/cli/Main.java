package ch.turic.cli;

import ch.turic.Interpreter;
import ch.turic.analyzer.Input;
import ch.turic.commands.operators.Cast;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

public class Main {

    final static Set<String> parameters = Set.of(
            // snippet command_options
            "version",
            "APPIA",
            "dry",
            "REPL",
            "help"
            // end snippet
    );

    public static void main(String[] args) throws IOException {
        final var params = CmdParser.parse(args, parameters);
        if (params.get("version").isPresent()) {

            var buildTime = new String(Main.class.getResourceAsStream("/buildtime.txt").readAllBytes(),StandardCharsets.UTF_8);
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
                    "  -dry                       compile only\n" +
                    "  -REPL                      start REPL" +
                    // end snippet
                    "");
            return;
        }
        if( params.get("REPL").isPresent() ) {
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





