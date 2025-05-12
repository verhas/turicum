package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.Command;

/**
 * Executes a previously deferred command produced by {@code thunk}.
 */
public class Unthunk implements TuriFunction {
    @Override
    public String name() {
        return "unthunk";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.oneArg(name(), args);
        final var arg = args[0];
        if (arg instanceof Command command) {
            return command.execute(ctx);
        }
        throw new ExecutionException("Cannot %s the value of %s", name(), arg);
    }

}
