package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.Closure;
import ch.turic.commands.Macro;

/**
 * Convert the argument to a lazy from a closure
 */
public class Reclose implements TuriFunction {
    @Override
    public String name() {
        return "reclose";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1, "Built-in function reclose needs exactly one argument");
        final var arg = args[0];
        if (arg instanceof Closure closure) {
            return new Closure(closure.name(), closure.parameters(), (ch.turic.memory.Context) context, closure.command());
        }
        if (arg instanceof Macro macro) {
            return new Macro(macro.name(),macro.parameters(), (ch.turic.memory.Context) context, macro.command());
        }
        throw new ExecutionException("Cannot get the lazy(%s) for thevalue of %s", arg.getClass().getCanonicalName(), arg);
    }
}
