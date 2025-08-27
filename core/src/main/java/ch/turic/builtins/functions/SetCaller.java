package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Set the value of a variable in the caller's context.
 * This function can be used in a function or closure that wants to alter some of the variables in the environment
 * it was invoked from.
 *
 * This is a very dangerous practice, and it is mainly to allow the development of decorators that modify named
 * functions or closures and then they want to alter the original variable holding these.
 */
public class SetCaller implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.twoArgs(name(), arguments);
        final var args = FunUtils.args(name(), arguments, String.class, Object.class);
        final var ctx = FunUtils.ctx(context);
        final var name = args.at(0).get().toString();
        final var value = args.at(1).get();
        ctx.caller().update(name,value);
        return value;
    }
}
