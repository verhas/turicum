package ch.turic.builtins.macros;

import ch.turic.*;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.FieldAccess;
import ch.turic.commands.Identifier;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import ch.turic.utils.StringUtils;

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
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var argO = FunUtils.oneOrMoreArgs(name(), arguments);
        final String arg;
        if (argO instanceof Command cmd) {
            arg = getImportString(cmd, ctx);
        } else {
            throw new ExecutionException("Import needs a string first argument");
        }
        Path sourceFile = locateSource(ctx, arg);

        try {
            final var source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            final var imports = getImportsList(arguments, ctx);
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
                imports.add(getImportString(command, ctx));
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
                if (StringUtils.matches(exported, k)) {
                    set.add(k);
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
        final String appiaSource;
        if (context.contains(APPIA)) {
            final var appia = context.get(APPIA);
            if (appia instanceof LngList appiaList) {
                appiaRoots = getAppiaRootsFrom(appiaList);
                appiaSource = "global variable";
            } else {
                throw new ExecutionException("There is an APPIA variable defined, but it is not a list. APPIA=%s", Objects.requireNonNullElse(appia, "none").toString());
            }
        } else {
            appiaRoots = this.appiaRoots;
            appiaSource = "environment variable";
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
            throw new ExecutionException("There is no import '%s' via APPIA(%s)=[%s], cwd=%s",
                    arg,
                    appiaSource,
                    String.join("|",appiaRoots.stream().map(Path::toString).toList()),
                    new File(".").getAbsolutePath());
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

    static String getImportString(Command cmd, ch.turic.memory.Context ctx) {
        if (cmd instanceof Identifier id) {
            return id.name();
        }
        if (cmd instanceof FieldAccess) {
            final var sb = new StringBuilder();
            var fa = cmd;
            while (fa instanceof FieldAccess f) {
                sb.insert(0, "." + f.identifier());
                fa = f.object();
            }
            if (fa instanceof Identifier id) {
                sb.insert(0, id.name());
                return sb.toString();
            }
        }
        return cmd.execute(ctx).toString();
    }
}
