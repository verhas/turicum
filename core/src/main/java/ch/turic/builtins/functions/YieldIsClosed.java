package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Evaluate a Command
 */
public class YieldIsClosed implements TuriFunction {
    @Override
    public String name() {
        return "yield_is_closed";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        if (!(context instanceof ch.turic.memory.Context ctx)) {
            throw new ExecutionException("context must be a context of type ch.turic.memory.Context");
        }
        ExecutionException.when(args.length > 0, "Built-in function %s needs no argument",name());
        return ctx.threadContext.yielder().toChild().isClosed();
    }

}
