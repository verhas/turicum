package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;

/**
 * The IsCurried class implements the TuriFunction interface and provides a function
 * to determine if a given object is in a "curried" state.
 * <p>
 * A curried object is defined as one that retains a partial set of arguments or
 * a reference to itself for future invocation.
 */
public class CurriedArity implements TuriFunction {
    @Override
    public String name() {
        return "curried_arity";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var closureLike = FunUtils.arg(name(), arguments, ClosureLike.class);
        return (long)(closureLike.getCurriedArguments() == null ? 0 : closureLike.getCurriedArguments().length);
    }
}
