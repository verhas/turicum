package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.TuriFunction;
import ch.turic.ExecutionException;
import ch.turic.memory.LngList;

import java.util.Collection;

/**
 * The {@code len()} function returns the len of a collection or a string. It is similar to the implementation of
 * Python's len.
 * <p>
 * For any Java collection, it will also return the size.
 * <pre>{@code
 * die "" if len([1,2,3]) != 3
 * die "" if len("abraka") != 6
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
            case LngList l -> l.array.size();
            case Object[] a -> a.length;
            case Collection<?> c -> c.size();
            default ->
                    throw new ExecutionException("Cannot get the len(%s) for the value of %s", arg.getClass().getCanonicalName(), arg);
        };
    }
}
