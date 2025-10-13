package ch.turic.builtins.macros;

import ch.turic.*;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.FieldAccess;
import ch.turic.commands.Identifier;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;
import ch.turic.memory.LocalContext;
import ch.turic.utils.AppiaHandler;
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

    private final AppiaHandler handler = new AppiaHandler();

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var argO = FunUtils.oneOrMoreArgs(name(), arguments);
        final String arg;
        if (argO instanceof Command cmd) {
            arg = getImportString(cmd, ctx);
        } else {
            throw new ExecutionException("import needs a string first argument");
        }
        final var sourceFile = handler.locateSource(ctx, arg);

        try {
            final var source = Files.readString(sourceFile, StandardCharsets.UTF_8);
            final var imports = getImportsList(arguments, ctx);
            return doImportExport(ctx, source, imports);
        } catch (IOException e) {
            throw new ExecutionException("Cannot read the import file '%s'", sourceFile.toString());
        }
    }

    static List<String> getImportsList(Object[] args, LocalContext ctx) {
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

    static Object doImportExport(LocalContext ctx, String source, List<String> imports) {
        final var interpreter = new Interpreter(source);
        final var program = interpreter.compile();
        final var importedContext = (LocalContext) interpreter.getImportContext();
        importedContext.globalContext.classLoader.inherit(ctx.globalContext.classLoader);
        interpreter.execute(program);
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


    static String getImportString(Command cmd, LocalContext ctx) {
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
