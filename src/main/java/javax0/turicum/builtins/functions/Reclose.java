package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.TuriFunction;
import javax0.turicum.commands.Closure;
import javax0.turicum.commands.Macro;

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
            return new Closure(closure.parameters(), (javax0.turicum.memory.Context) context, closure.command());
        }
        if (arg instanceof Macro macro) {
            return new Macro(macro.parameters(), (javax0.turicum.memory.Context) context, macro.command());
        }
        throw new ExecutionException("Cannot get the lazy(%s) for the value of %s", arg.getClass().getCanonicalName(), arg);
    }
}
