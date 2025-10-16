package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;
/*snippet builtin0370

=== `set`, `set_force`

Set the value of a variable in the local context.
This function is handy for meta-programming when the variable's name is available as a string.
An example is in the sample:

{%S fnDecorator3%}

It uses this function to decorate a function and then set the variable of the same name, overwriting the definition with a new, decorated closure.

Note that the example that way works only when the decorating closure is defined in the same context as the decorated function or in a subcontext.
This way, the closure sees the function symbol `q` and it can redefine it.
A more robust approach to creating decorators involves calling `set_caller_force`.

{%FORCE%}



end snippet */

/**
 * Set the value of a variable.
 * This function is to be used when the name of a variable is not available at compile time and can only be provided as a string.
 * <p>
 * Calling this function {@code set("name",value)} is the same as the command {@code name = value}.
 */
@Name("set")
public class SetSymbol implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.twoArgs(name(), arguments);
        final var args = FunUtils.args(name(), arguments, String.class, Object.class);
        final var ctx = FunUtils.ctx(context);
        final var name = args.at(0).get().toString();
        final var value = args.at(1).get();
        ctx.update(name, value);
        return value;
    }
}
