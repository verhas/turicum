package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.Interpreter;
import ch.turic.TuriMacro;
import ch.turic.Command;
import ch.turic.commands.Identifier;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * It loads the file based on the APPIA environment variable or .env file.
 */
public class Import implements TuriMacro {

    public static final String APPIA = "APPIA";
    private final List<Path> appiaRoots = getAppiaRoots();

    @Override
    public String name() {
        return "import";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var argO = FunUtils.oneOrMoreArgs(name(), args);
        final String arg;
        if (argO instanceof Command cmd) {
            arg = cmd.execute(ctx).toString();
        } else {
            throw new ExecutionException("Import needs a string first argument");
        }
        Path sourceFile = locateSource(ctx, arg);

        try {
            final var source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            final var imports = getImportsList(args, ctx);
            return doImportExport(ctx, source, imports);
        } catch (IOException e) {
            throw new ExecutionException("Cannot read the import file '%s'", sourceFile.toString());
        }
    }

    static List<String> getImportsList(Object[] args, ch.turic.memory.Context ctx) {
        final var imports = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            if (args[i] instanceof Identifier id) {
                imports.add(id.name());
            } else if (args[i] instanceof Command command) {
                imports.add(command.execute(ctx).toString());
            }
        }
        return imports;
    }

    static Object doImportExport(ch.turic.memory.Context ctx, String source, List<String> imports) {
        final var interpreter = new Interpreter(source);
        interpreter.compileAndExecute();
        final var importedContext = (ch.turic.memory.Context) interpreter.getImportContext();
        final var set = new HashSet<String>();
        for (final var exported : (imports == null || imports.isEmpty()) ? importedContext.exporting() : imports) {
            for (final var k : importedContext.keys()) {
                if (matches(exported, k)) {
                    set.add(k);
                    break;
                }
            }
        }
        for (final var exported : set) {
            ctx.let0(exported, importedContext.get(exported));
        }
        return new LngObject(null, importedContext);
    }

    private Path locateSource(ch.turic.memory.Context context, String arg) {
        final var relativePath = Path.of(arg.replace('.', File.separatorChar) + ".turi");

        final List<Path> appiaRoots;
        if (context.contains(APPIA)) {
            final var appia = context.get(APPIA);
            if (appia instanceof LngList appiaList) {
                appiaRoots = getAppiaRootsFrom(appiaList);
            } else {
                throw new ExecutionException("There is an APPIA variable defined, but it is not a list. APPIA=%s", Objects.requireNonNullElse(appia, "none").toString());
            }
        } else {
            appiaRoots = this.appiaRoots;
        }
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

    private List<Path> getAppiaRootsFrom(LngList appiaList) {
        final List<Path> appiaRoots;
        appiaRoots = toPathList(appiaList.array.stream()
                .map(o -> Objects.requireNonNullElse(o, "none"))
                .map(Object::toString));
        return appiaRoots;
    }

    private static List<Path> toPathList(Stream<String> stringStream) {
        return stringStream.map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Path::of)
                .map(Path::normalize)
                .map(Path::toAbsolutePath)
                .toList();

    }

    private static List<Path> getAppiaRoots() {
        var appia = System.getProperty(APPIA);
        if (appia == null) {
            System.getenv(APPIA);
        }
        if (appia == null) {
            appia = loadFromEnvFile();
        }
        if (appia == null || appia.isBlank()) {
            return List.of();
        }

        return toPathList(Arrays.stream(appia.split("\\|")));
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
                        if (trimmed.startsWith(APPIA + "=")) {
                            return trimmed.substring((APPIA + "=").length()).trim();
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

    public static boolean matches(String pattern, String text) {
        return matchHelper(pattern, 0, text, 0);
    }

    private static boolean matchHelper(String pattern, int pIdx, String text, int tIdx) {
        // End of a pattern
        if (pIdx == pattern.length()) {
            return tIdx == text.length();
        }

        // If the current pattern char is '*'
        if (pattern.charAt(pIdx) == '*') {
            // Try to match '*' with 0 or more characters
            for (int k = tIdx; k <= text.length(); k++) {
                if (matchHelper(pattern, pIdx + 1, text, k)) {
                    return true;
                }
            }
            return false;
        }

        // Normal character must match
        if (tIdx < text.length() && pattern.charAt(pIdx) == text.charAt(tIdx)) {
            return matchHelper(pattern, pIdx + 1, text, tIdx + 1);
        }

        return false;
    }


}
