package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.Closure;

/**
 * Convert the argument to a macro from a closure
 */
public class Macro implements TuriFunction {
    @Override
    public String name() {
        return "macro";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1, "Built-in function macro needs exactly one argument");
        final var arg = args[0];
        if (arg instanceof Closure closure) {
            return new ch.turic.commands.Macro(closure.parameters(), closure.wrapped(), closure.command());
        }
        throw new ExecutionException("Cannot get the macro(%s) for the value of %s", arg.getClass().getCanonicalName(), arg);
    }
}
