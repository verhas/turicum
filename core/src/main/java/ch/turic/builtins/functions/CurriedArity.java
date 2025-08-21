package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;

/**
 *
 * The built-in function `curried_arity()` returns the number of curried arguments.
 *
 *<pre>{@code
 * fn x(a,b,c){}
 * let k = x.(13)
 * die "" if curried_arity(k) != 1
 * die "" if arity(k) != 3
 * }</pre>
 *
 * In the above example {@code k} is a function created currying the first argument of the function {@code x}.
 * Since there is one argument curried, the value of the call to `curried_arity()` is one.
 * The full arity of the function {@code k} is still three.
 * If you want to know how many parameters you need to provide, you have to calculate the difference.
 *
 */
public class CurriedArity implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var closureLike = FunUtils.arg(name(), arguments, ClosureLike.class);
        return (long)(closureLike.getCurriedArguments() == null ? 0 : closureLike.getCurriedArguments().length);
    }
}
