package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.TuriFunction;
import ch.turic.ExecutionException;
import ch.turic.memory.LngList;

import java.util.Collection;
/*snippet builtin0240

=== `len`

This function returns the length of the argument.

The argument can be:

* string, the value is the length of the string.

* list, the value is the number of elements in the list.

* Java array, the value is the array length.

* Java collection, the value is the size of the collection.

{%S len%}

end snippet */

/**
 * The {@code len()} function returns the len of a collection or a string. It is similar to the implementation of
 * Python's len.
 * <p>
 * For any Java collection, it will also return the size.
 * <pre>{@code
 * die "" when len([1,2,3]) != 3
 * die "" when len("abraka") != 6
 * }</pre>
 *
 */
public class Len implements TuriFunction {
    @Override
    public String name() {
        return "len";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        return (long) switch (arg) {
            case String s -> s.length();
            case LngList l -> l.size();
            case Object[] a -> a.length;
            case Collection<?> c -> c.size();
            default ->
                    throw new ExecutionException("Cannot get the len(%s) for the value of %s", arg.getClass().getCanonicalName(), arg);
        };
    }
}
