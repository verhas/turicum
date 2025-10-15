package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0360
=== `set_caller`, `set_caller_force`

This function sets the variable in the caller context.

This is particularly useful when implementing a decorator that redefines a function or class,
especially when the decorator operates in a different context than the decorated function.

Calling this function from inside another function can redefine a variable in the context of the calling function or its closure.

{%FORCE%}

{%S fnDecorator5%}

Note that the example uses the `_force` version of the function.
Named functions assign the function to a symbol, such as `q` in this example, and then pin these symbols.
It is an error to define a function in a context where it was already defined.
end snippet */

/**
 * Set the value of a variable in the caller's context.
 * This function can be used in a function or closure that wants to alter some of the variables in the environment
 * it was invoked from.
 * <p>
 * This is a very dangerous practice, and it is mainly to allow the development of decorators that modify named
 * functions or closures, and then they want to alter the original variable holding these.
 */
public class SetCaller implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.twoArgs(name(), arguments);
        final var args = FunUtils.args(name(), arguments, String.class, Object.class);
        final var ctx = FunUtils.ctx(context);
        final var name = args.at(0).get().toString();
        final var value = args.at(1).get();
        ctx.caller().update(name, value);
        return value;
    }
}
