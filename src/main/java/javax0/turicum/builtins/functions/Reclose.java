package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.TuriFunction;
import javax0.turicum.commands.Closure;
import javax0.turicum.commands.Macro;

/**
 * Convert the argument to a macro from a closure
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
        if (arg instanceof Closure(
                String[] parameters, javax0.turicum.memory.Context ignore, javax0.turicum.commands.BlockCommand command
        )) {
            return new Closure(parameters, (javax0.turicum.memory.Context) context, command);
        }
        if (arg instanceof Macro(
                String[] parameters, javax0.turicum.memory.Context ignore, javax0.turicum.commands.BlockCommand command
        )) {
            return new Macro(parameters, (javax0.turicum.memory.Context) context, command);
        }
        throw new ExecutionException("Cannot get the macro(%s) for the value of %s", arg.getClass().getCanonicalName(), arg);
    }
}
