package ch.turic.clifx;

import ch.turic.Input;
import ch.turic.Interpreter;
import javafx.application.Application;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;

public class Main extends Application {

    private static String[] args;

    public static void main(String[] args) {
        Main.args = args;
        launch(args);
    }

    @Override
    public void start(javafx.stage.Stage stage) {
        if (args.length == 0) {
            System.err.println("No input file specified error and print help file");
        }
        try (final var interpreter = new Interpreter(Input.fromFile(Path.of(args[0])))) {
            final var injectedVariables = new HashMap<String, Object>();
            injectedVariables.put("fx_stage", stage);
            final var result = interpreter.compileAndExecute(injectedVariables);
        } catch (IOException e) {
            System.err.println("Error reading input file: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}





