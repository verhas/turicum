package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.LngCallable;
import ch.turic.TuriFunction;
import ch.turic.commands.Closure;
import ch.turic.ExecutionException;
import ch.turic.commands.Macro;
import ch.turic.memory.*;

/**
 * Return the type of the argument as a string.
 * <p>
 * The type is represented by one of the type names, like {@code bool}, {@code macro} etc.
 * <p>
 * The type of {@code none} is {@code none}. This is the only value for which the {@code type()} is not a string.
 * <p>
 * General Java types are represented by the canonical name with an extra {@code java.} prefix.
 * It may seem redundant when you look at classes from the JDK that already start with {@code java.} prefix, like
 * {@code java.lang.Integer} that will be {@code java.java.lang.Integer}.
 */
public class Type implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        return switch (arg) {
            case Boolean ignore -> "bool";
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
