package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.LngCallable;
import ch.turic.TuriFunction;
import ch.turic.commands.Closure;
import ch.turic.ExecutionException;
import ch.turic.memory.LngClass;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

/**
 * Return the types of the argument as a string.
 */
public class Type implements TuriFunction {
    @Override
    public String name() {
        return "type";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1, "Built-in function types needs exactly one argument");
        final var arg = args[0];
        return switch (arg) {
            case LngClass ignore -> "CLASS";
            case LngList ignore -> "LIST";
            case LngObject object -> object.lngClass().name();
            case Closure closure -> {
                if (closure.wrapped() == null) {
                    yield "FUNCTION";
                } else {
                    yield "CLOSURE";
                }
            }
            case LngCallable ignore -> "FUNCTION";
            case null -> "none";
            default -> "JAVA#" + arg.getClass().getCanonicalName();
        };
    }
}
