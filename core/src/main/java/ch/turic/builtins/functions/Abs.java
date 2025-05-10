package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;

/**
 * Get the absolute value of the argument
 */
public class Abs implements TuriFunction {
    @Override
    public String name() {
        return "abs";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        FunUtils.oneArg(name(), args);
        final var arg = args[0];
        if (Cast.isLong(arg)) {
            return Math.abs(Cast.toLong(arg));
        }
        if (Cast.isDouble(arg)) {
            return Math.abs(Cast.toDouble(arg));
        }
        throw new ExecutionException("%s argument is not a long/double", name());
    }
}
