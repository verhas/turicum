package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.AbstractCommand;

/**
 * returns the argument as a command converted to an LngObject
 */
public class Command implements TuriFunction {

    @Override
    public String name() {
        return "as_object";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var arg = FunUtils.oneArg(name(), args);
        if (arg instanceof AbstractCommand cmd) {
            return cmd.toLngObject(ctx);
        } else {
            throw new ExecutionException("%s argument has to be an command, got '%s'", name(), arg);
        }
    }
}
