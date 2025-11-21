package ch.turic.builtins.macros;

import ch.turic.*;
import ch.turic.builtins.functions.FunUtils;
import ch.turic.commands.FieldAccess;
import ch.turic.commands.Identifier;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LngObject;
import ch.turic.memory.LocalContext;
import ch.turic.utils.AppiaHandler;
import ch.turic.utils.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
/*snippet builtin0125

=== `import`

Import allows you to get definitions from external files into your code.

When you have an extensive application, you can separate some of the function definitions or classes into separate files.
These separate files can be referenced, and their definitions can be loaded into the currently running interpreter.

The function `import` searches the file, looking at each directory listed in the environment variable `APPIA`.

[NOTE]
====
The application

* first looks at the variable named `APPIA` (either global or local),
* then it tries the Java system property named `APPIA`,
* then the environment variable, and finally
* it tries to load the search path from the local directory `.env` file.

Handling the `.env` file is implemented in the code; there is no need to use external helper tools.
====

[NOTE]
====
JavaScript, Python, and some other scripting languages differentiate between static and dynamic importing.
Due to its fully dynamic nature, all imports are dynamic in Turicum.
====

`APPIA` is a string that lists the directories where the imported files may be.
The individual directories are separated using `|` characters.

[NOTE]
====
The character `|` was selected because it works on Windows as well as on Linux and on other Unix-based operating systems.
The drawback is that the shell may interpret it, but the value of `APPIA` is rarely defined on the command line.
====

When the import function encounters a request to import the file `a.b.c.d`, it will examine the subdirectory `a/b/c` for each `APPIA` directory.
The search is performed from left to right, as defined by the directories in `APPIA`.
The first location where the  `d.turi` file is found "wins" and the file is loaded.

Technically, the function

* loads a Turicum source file,
* executes using a separate interpreter, and
* returns the global context of the interpreter as a class-less object.

If you have function or class definitions in the imported file, they will be fields in the returned object.
You can assign the returned object to a variable or use it immediately, selecting specific fields as needed.
If you want a specific class or function in your namespace, you can `let` assign it to a variable.

The following is a file that is imported by the sample after it.

.file `import_this.turi`
[source]
----
{%@include [verbatim] ./core/src/test/resources/import_this.turi%}
----

{%S import%}

There are two functions related to the macro `import`:

* `export()`, and
* `export_all()`.

When these are used in an imported file, the variables from the imported file are copied automatically to the importing context.
With `export()` you can list the symbols that you want to be exported.
The function `export_all()` will export all the global symbols from the imported file, except the predefined ones.
Those are defined already in the importing context, and it would be dangerous to redefine things like `true`, `false`, and built-in functions using an import; hence, it is not done.

Specifying extra arguments after the imported file name can define the symbols you want to import.
These arguments can be identifiers and/or expressions resulting in a string.
If there is such a list in the import function, the `export()` or `export_all()` in the imported file is ignored.

{%S limited_import%}

Also, when you specify the name of the imported program, you can specify it as a string or as a series of identifiers with dots.


end snippet*/

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
            return doImportExport(ctx, source, imports, sourceFile);
        } catch (IOException e) {
            throw new ExecutionException("Cannot read the import file '%s'", sourceFile.toString());
        }
    }

    /**
     * Generates a list of import strings based on the provided arguments and context.
     * The arguments can be identifiers, field access, like {@code turi.debug}, or commands that result something
     * that can be converted to a string. (Everything can be converted to a string, except 'none'.)
     *
     * @param args the array of arguments as passed to the macro
     * @param ctx  the local context used to process commands or other related information
     * @return a list of strings representing the imports extracted or generated from the input arguments and context
     */
    static List<String> getImportsList(Object[] args, LocalContext ctx) {
        final var imports = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            if (args[i] instanceof Command command) {
                imports.add(getImportSymbol(command, ctx));
            }
        }
        return imports;
    }

    static Object doImportExport(LocalContext ctx, String source, List<String> imports, final Path sourceFile) {
        final var interpreter = new Interpreter(Input.fromString(source, sourceFile.toString()));
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
            if (ctx.contains(exported)) {
                throw new ExecutionException("Variable '%s' is already defined, while importing from %s", exported, sourceFile.toString());
            }
            ctx.let0(exported, importedContext.get(exported));
        }
        return new LngObject(null, importedContext);
    }


    /**
     * Constructs an import string representation for the given command.
     * Converts the command to an appropriate import string by extracting identifiers
     * or field access paths, or by invoking the execution of the command within the provided context.
     * <p>
     * The return value will be a string representing the identifier or field access structure as in the source code.
     * In a way, this code disassembles the field access, for example
     *
     * <pre>
     * {@code
     * a -> "a"
     * turi.debug -> "turi.debug"
     * }</pre>
     *
     * @param cmd the command to be processed for generating the import string
     * @param ctx the local context used during command execution
     * @return the import string constructed from the command
     */
    static String getImportString(Command cmd, LocalContext ctx) {
        if (cmd instanceof Identifier id) {
            return id.name();
        }
        if (cmd instanceof FieldAccess) {
            final var sb = new StringBuilder();
            var fa = cmd;
            // Note that you get the rightmost identifier first with this code.
            // The reverse string builder order is used to construct the field access path
            // as it was originally in the source code.
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

    static String getImportSymbol(Command cmd, LocalContext ctx) {
        return cmd instanceof Identifier id ? id.name() : cmd.execute(ctx).toString();
    }
}
