package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.ClosureLike;
/*snippet builtin0060

=== `curried_arity`

The built-in function `curried_arity()` returns the number of curried arguments.

When you curry a function, then the arity of the resulting function decreases by the number of curried arguments.
The function `arity()` returns the full arity of the function.

{%S curried_arity%}

The function `add()` in the example adds two numbers.
The arity of the function is two, as it has two arguments.
The function `add5()` curries one of the arguments.
The arity of this new function is still two in Turicum, but one argument is now curried.
The number of the curried arguments is returned by `curried_arity()`.

end snippet */
/**
 *
 * The built-in function `curried_arity()` returns the number of curried arguments.
 *
 *<pre>{@code
 * fn x(a,b,c){}
 * let k = x.(13)
 * die $"curried_arity(k) is $(curried_arity(k)) not 1" when curried_arity(k) != 1
 * die $"arity(k) os $(arity(k)) not 3" when arity(k) != 3
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
