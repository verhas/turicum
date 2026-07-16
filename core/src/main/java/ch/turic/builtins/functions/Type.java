package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.LngCallable;
import ch.turic.TuriFunction;
import ch.turic.analyzer.Types;
import ch.turic.commands.Closure;
import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.Macro;
import ch.turic.memory.*;
/*snippet builtin0490


=== `type`

This method returns the type of the argument as a string.

{%S type1%}

When the argument is a Java object, then the type is `java.` + the canonical name of the class.

end snippet */

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
            case Boolean ignore -> Types.BOOL;
            case Macro ignore -> Types.MACRO;
            case LngClass ignore -> Types.CLS;
            case LngList ignore -> Types.LST;
            case String ignore -> Types.STR;
            case byte[] ignore -> Types.BIN;
            case Double ignore -> Types.FLOAT;
            case Long ignore -> Types.INT;
            case Number ignore -> Types.NUM;
            case LngException ignore -> Types.ERR;
            case LngObject object -> object.lngClass() == null ? Types.OBJ : object.lngClass().name();
            case Closure ignore -> Types.FN;
            case LngCallable ignore -> Types.FN;
            case Channel<?> ignore -> Types.QUE;
            case AsyncStreamHandler ignore -> Types.TASK;
            case null -> Types.NONE;
            default -> "java." + arg.getClass().getCanonicalName();
        };
    }
}
