package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * The ExportAll class implements the TuriFunction interface to provide a function
 * for exporting all entries from the context. This function is part of the Turi
 * language's built-in functionality and is identified by the name "export_all".
 * <p>
 * The `call` method retrieves the execution context, validates that no arguments
 * are provided, iterates over the keys in the context, and adds each key to the
 * export list.
 * <p>
 * The ExportAll function ensures that all variables or entries in the context
 * are marked for export with minimal user interaction.
 * <p>
 * Methods:
 * - name(): Returns the function's unique identifier name, "export_all".
 * - call(Context context, Object[] args): Executes the export operation by
 * marking all keys in the context for export. This method validates that no
 * arguments are passed and throws an ExecutionException in case of invalid usage.
 */
public class ExportAll implements TuriFunction {

    @Override
    public String name() {
        return "export_all";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.noArg(name(), arguments);
        for (final var key : ctx.keys()) {
            ctx.addExport(key);
        }
        return null;
    }
}
