package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Set the value of a variable.
 * This function is to be used when the name of a variable is not available at compile time and can only be provided as a string.
 *
 * Calling this function {@code set("name",value)} is the same as the command {@code name = value}.
 */
public class Set implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.twoArgs(name(), arguments);
        final var args = FunUtils.args(name(), arguments, String.class, Object.class);
        final var ctx = FunUtils.ctx(context);
        final var name = args.at(0).get().toString();
        final var value = args.at(1).get();
        ctx.update(name,value);
        return value;
    }
}
