package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

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
            ctx.addExport(key);
        }
        return null;
    }
}
