package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.Command;
import ch.turic.builtins.functions.FunUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static ch.turic.builtins.macros.Import.doImportExport;
import static ch.turic.builtins.macros.Import.getImportsList;

/**
 * Imports a file as turi source code from the classpath.
 */
public class SysImport implements TuriMacro {

    @Override
    public String name() {
        return "sys_import";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var argO = FunUtils.oneOrMoreArgs(name(), arguments);
        final String sys_name;
        if (argO instanceof Command cmd) {
            sys_name = Import.getImportString(cmd,ctx);
        } else {
            throw new ExecutionException("Import needs a string first argument");
        }
        final var resourceName = sys_name.replace(".", "/") + ".turi";

        try (final var is = this.getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new ExecutionException("Could not find sys import " + sys_name + " in " + resourceName);
            }
            final var source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            final var imports = getImportsList(arguments, ctx);
            return doImportExport(ctx, source, imports);
        } catch (IOException e) {
            throw new ExecutionException("Cannot read the sys import '%s'", sys_name);
        }
    }

}
