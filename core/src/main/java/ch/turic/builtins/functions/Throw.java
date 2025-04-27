package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Implement the len function more or less as it is implemented in Python.
 * Return the length of a string or a length of a list, which you can get anyway using the 'length' field.
 * For any Java collection it will also return the size.
 */
public class Throw implements TuriFunction {
    @Override
    public String name() {
        return "die";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 1, "Built-in function %s needs exactly one argument", name());
        final var arg = args[0].toString();
        throw new ExecutionException(arg);
    }
}
