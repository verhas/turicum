package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Returns true if the yield channel is closed
 */
public class YieldIsClosed implements TuriFunction {
    @Override
    public String name() {
        return "yield_is_closed";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.noArg(name(), arguments);
        return ctx.threadContext.yielder().toChild().isClosed();
    }
}
