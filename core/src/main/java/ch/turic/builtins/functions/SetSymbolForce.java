package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;

/**
 * Set the value of a variable, and go on even if it is pinned.
 * This function is needed to alter functions by decorators. Functions are pinned when defined.
 * <p>
 * Calling this function {@code set_force("name",value)} is the same as the command {@code name = value}, but works
 * even when the variable is pinned.
 */
@Name("set_force")
public class SetSymbolForce implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.twoArgs(name(), arguments);
        final var args = FunUtils.args(name(), arguments, String.class, Object.class);
        final var ctx = FunUtils.ctx(context);
        final var name = args.at(0).get().toString();
        final var value = args.at(1).get();
        ctx.updateForce(name, value);
        return value;
    }
}
