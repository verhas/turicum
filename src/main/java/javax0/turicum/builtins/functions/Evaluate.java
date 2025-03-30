package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.TuriFunction;
import javax0.turicum.commands.Command;

/**
 * Evaluate a Command
 */
public class Evaluate implements TuriFunction {
    @Override
    public String name() {
        return "evaluate";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1, "Built-in function evaluate needs exactly one argument");
        final var arg = args[0];
        if (arg instanceof Command command) {
            return command.execute((javax0.turicum.memory.Context) context);
        }
        throw new ExecutionException("Cannot get the macro(%s) for the value of %s", arg.getClass().getCanonicalName(), arg);
    }
}
