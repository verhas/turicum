package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.Interpreter;
import javax0.turicum.TuriFunction;
import javax0.turicum.memory.LngObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Convert the argument to a macro from a closure.
 * <p>
 * It loads the file based on the APPIA environment variable or .env file.
 */
public class Import implements TuriFunction {

    private final List<Path> appiaRoots = getAppiaRoots();

    @Override
    public String name() {
        return "import";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1 || args[0] == null, "Built-in function import needs exactly one argument");
        final var arg = args[0].toString();
        Path sourceFile = locateSource(arg);

        try {
            final var source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            final var interpreter = new Interpreter(source);
            interpreter.execute();
            return new LngObject(null, (javax0.turicum.memory.Context) interpreter.getImportContext());
        } catch (IOException e) {
            throw new ExecutionException("Cannot read the import file '%s'", sourceFile.toString());
        }
    }

    private Path locateSource(String arg) {
        final var relativePath = Path.of(arg.replace('.', File.separatorChar) + ".turi");

        Path sourceFile = null;
        for (var root : appiaRoots) {
            var candidate = root.resolve(relativePath);
            if (Files.exists(candidate)) {
                sourceFile = candidate;
                break;
            }
        }

        if (sourceFile == null) {
            throw new ExecutionException("There is no import '%s' via APPIA", arg);
        }
        return sourceFile;
    }

    private static List<Path> getAppiaRoots() {
        var appia = System.getenv("APPIA");
        if (appia == null) {
            appia = loadFromEnvFile();
        }
        if (appia == null || appia.isBlank()) {
            return List.of();
        }

        return Arrays.stream(appia.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Path::of)
                .map(Path::normalize)
                .map(Path::toAbsolutePath)
                .toList();
    }

    private static String loadFromEnvFile() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            try {
                Path envPath = current.resolve(".env");
                if (Files.exists(envPath) && Files.isReadable(envPath)) {
                    var lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
                    for (var line : lines) {
                        var trimmed = line.trim();
                        if (trimmed.startsWith("APPIA=")) {
                            return trimmed.substring("APPIA=".length()).trim();
                        }
                    }
                }
            } catch (IOException ignored) {
            } finally {
                current = current.getParent();
            }
        }
        return null;
    }

}
