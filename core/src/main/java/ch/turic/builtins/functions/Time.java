package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Implement the len function more or less as it is implemented in Python.
 * Return the length of a string or a length of a list, which you can get anyway using the 'length' field.
 * For any Java collection it will also return the size.
 */
public class Time implements TuriFunction {
    @Override
    public String name() {
        return "time";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.noArg(name(), arguments);
        return System.currentTimeMillis();
    }

}
