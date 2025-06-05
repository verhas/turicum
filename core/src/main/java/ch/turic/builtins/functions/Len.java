package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.TuriFunction;
import ch.turic.ExecutionException;
import ch.turic.memory.LngList;

import java.util.Collection;

/**
 * Implement the len function more or less as it is implemented in Python.
 * Return the length of a string or a length of a list, which you can get anyway using the 'length' field.
 * For any Java collection it will also return the size.
 */
public class Len implements TuriFunction {
    @Override
    public String name() {
        return "len";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        return (long)switch (arg){
            case String s -> s.length();
            case LngList l -> l.array.size();
            case Object[] a -> a.length;
            case Collection<?> c -> c.size();
            default -> throw new ExecutionException("Cannot get the len(%s) for the value of %s",arg.getClass().getCanonicalName(),arg);
        };
    }
}
