package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0380
=== `set_global`

It is similar to `set()`, but it sets the value of a global variable.
Note that there is no `*_force` version of this function.
Pinning is always local, and this method works directly on the global variables.

end snippet */

/**
 * Set the value of a variable. It is the same as the function {@link SetSymbol} but it does set a global variable.
 */
public class SetGlobal implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.twoArgs(name(), arguments);
        final var args = FunUtils.args(name(), arguments, String.class, Object.class);
        final var ctx = FunUtils.ctx(context);
        final var name = args.at(0).get().toString();
        final var value = args.at(1).get();
        ctx.global(name,value);
        return value;
    }
}
