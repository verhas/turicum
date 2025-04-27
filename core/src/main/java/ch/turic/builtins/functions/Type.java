package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.LngCallable;
import ch.turic.TuriFunction;
import ch.turic.commands.Closure;
import ch.turic.ExecutionException;
import ch.turic.commands.Macro;
import ch.turic.memory.*;

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
        final var arg = FunUtils.oneArg(name(),args);
        return switch (arg) {
            case Macro ignore -> "macro";
            case LngClass ignore -> "cls";
            case LngList ignore -> "lst";
            case String ignore -> "str";
            case Double ignore -> "float";
            case Long ignore -> "num";
            case LngException ignore -> "err";
            case LngObject object -> object.lngClass() == null ? "obj" : object.lngClass().name();
            case Closure ignore -> "fn";
            case LngCallable ignore -> "fn";
            case Channel<?> ignore -> "que";
            case AsyncStreamHandler ignore -> "task";
            case null -> "none";
            default -> "java." + arg.getClass().getCanonicalName();
        };
    }
}
