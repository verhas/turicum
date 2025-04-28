package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * sets all the names as exported
 */
public class ExportAll implements TuriFunction {

    @Override
    public String name() {
        return "export_all";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.noArg(name(), args);
        for (final var key : ctx.keys()) {
            ctx.addExport(key);
        }
        return null;
    }
}
