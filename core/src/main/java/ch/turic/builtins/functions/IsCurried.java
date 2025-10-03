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
 *
 * <pre>{@code
 * fn x(a,b){}
 * let k = x.()
 * die "" when ! is_curried(k) || curried_arity(k) != 0
 * die "" when is_curried(x) || curried_arity(x) != 0
 * }</pre>
 *
 * {@code k} is a curried function even though zero arguments are curried. You cannot simply rely on the number
 * of the curried arguments to know if a function is curried or not. Bot {@code k} and {@code x} have zero curried
 * arguments in the example, but only one is curried.
 *
 */
public class IsCurried implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var closureLike = FunUtils.arg(name(), arguments, ClosureLike.class);
        return closureLike.getCurriedSelf() != null || closureLike.getCurriedArguments() != null;
    }
}
