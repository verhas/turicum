package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.LngCallable;
import javax0.turicum.TuriFunction;
import javax0.turicum.commands.Closure;
import javax0.turicum.ExecutionException;
import javax0.turicum.memory.LngClass;
import javax0.turicum.memory.LngList;
import javax0.turicum.memory.LngObject;

/**
 * Return the type of the argument as a string.
 */
public class Type implements TuriFunction {

    @Override
    public String name() {
        return "type";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1, "Built-in function type needs exactly one argument");
        final var arg = args[0];
        return switch (arg) {
            case LngClass ignore -> "CLASS";
            case LngList ignore -> "LIST";
            case LngObject object -> object.lngClass().name();
            case LngCallable ignore -> "FUNCTION";
            case Closure closure -> {
                if (closure.wrapped() == null) {
                    yield "FUNCTION";
                } else {
                    yield "CLOSURE";
                }
            }
            case null -> "none";
            default -> "JAVA#" + arg.getClass().getCanonicalName();
        };
    }

}
