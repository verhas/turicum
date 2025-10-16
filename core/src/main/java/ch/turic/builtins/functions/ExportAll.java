package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0100

=== `export`, `export_all`

These are two functions to be used in files imported.
The macro `export` will export the variables listed as arguments.
That way, these variables will be copied into the context of the importing code.

You can specify identifiers and/or expressions resulting in strings as arguments.

NOTE: Technically, `export` is a macro, recognizing variables instead of their values as arguments.
If you have a variable that contains the name of the symbol you want to export use `export (variable)` to force the evaluation
instead of `export variable`, exporting the variable itself.

`export_all()` will export all the variables from the importing context.
Note that in the case of `export_all()`, you cannot omit the `(` and `)`.

end snippet */

/**
 * Export all is the function that exports all symbols form the current context to the parent context.
 * It is usually used in imported files to export all the defined symbols.
 */
public class ExportAll implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.noArg(name(), arguments);
        for (final var key : ctx.keys()) {
            if (!ctx.globalContext.predefinedGlobals.contains(key)) {
                ctx.addExport(key);
            }
        }
        return null;
    }
}
