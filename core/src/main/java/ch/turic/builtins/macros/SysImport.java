package ch.turic.builtins.macros;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.Command;
import ch.turic.builtins.functions.FunUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static ch.turic.builtins.macros.Import.doImportExport;
import static ch.turic.builtins.macros.Import.getImportsList;
/*snippet builtin0444

=== `sys_import`

This command can be used to import Turicum system files.

The Turicum run-time and other libraries supporting a Turicum program can deliver Turicum source code files in the Java resource path.
Using this macro, you can import those files as if they were in a directory listed in the APPIA path.

The Turicum run-time itself provides such files

* to handle Maven POM files when Turicum is used for Maven projects,
* support debugging Turicum programs,
* io support,
* measurement units,

and some others.

You can use `sys_import` in the same way as `import`,
either specifying only the name of the import resource,
or specifying the name and the list of imported symbols.

Use `.` as a separator when importing from a package.
For example, the Turicum run-time in the Turicum JAR file is in the directory `classes/turi`.
If you want to import the resource from the file `appia.turi` you can do it as

    sys_import turi.appia

In this case, `turi` at the start is the directory name inside the JAR file under `classes`, and `appia` is the file name.
The extension `.turi` is appended automatically in the same way as for the `import` command.

end snippet*/
/**
 * Imports a file as turi source code from the classpath.
 */
public class SysImport implements TuriMacro {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var argO = FunUtils.oneOrMoreArgs(name(), arguments);
        final String sys_name;
        if (argO instanceof Command cmd) {
            sys_name = Import.getImportString(cmd,ctx);
        } else {
            throw new ExecutionException("sys_import needs a string first argument");
        }
        final var resourceName = sys_name.replace(".", "/") + ".turi";

        try (final var is = ctx.globalContext.classLoader.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new ExecutionException("Could not find sys import " + sys_name + " in " + resourceName);
            }
            final var source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            final var imports = getImportsList(arguments, ctx);
            return doImportExport(ctx, source, imports, Path.of(resourceName));
        } catch (IOException e) {
            throw new ExecutionException("Cannot read the sys import '%s'", sys_name);
        }
    }

}
