package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.TuriFunction;
import ch.turic.commands.Closure;
import ch.turic.commands.Macro;
import ch.turic.memory.*;

/**
 * Return the types of the argument as a string.
 */
public class IsObject implements TuriFunction {
    @Override
    public String name() {
        return "is_obj";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        return FunUtils.oneArg(name(),args) instanceof LngObject;
    }
}
