package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0200

=== `java_type`

Returns the canonical name of the Java class of the argument.

{%S java_type%}

end snippet */

/**
 * Return the Java type of the argument as a string.
 * <p>
 * The returned string does not contain the {@code java.} prefix.
 */
public class JavaType implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        return arg.getClass().getCanonicalName();
    }
}
