package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.Command;

/**
 * Executes a previously deferred command produced by {@code thunk}.
 */
public class Unthunk implements TuriFunction {
    @Override
    public String name() {
        return "unthunk";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var arg = FunUtils.arg(name(), arguments);
        if (arg instanceof Command command) {
            return command.execute(ctx);
        }
        throw new ExecutionException("Cannot %s the value of %s", name(), arg);
    }

}
