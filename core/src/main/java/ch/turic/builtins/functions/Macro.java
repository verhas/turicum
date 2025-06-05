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
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var closure = FunUtils.arg(name(), arguments, Closure.class);
        return new ch.turic.commands.Macro(closure.name(), closure.parameters(), closure.wrapped(), closure.command());
    }
}
