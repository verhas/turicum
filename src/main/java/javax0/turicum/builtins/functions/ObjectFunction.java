package javax0.turicum.builtins.functions;

import javax0.turicum.Context;
import javax0.turicum.ExecutionException;
import javax0.turicum.TuriFunction;
import javax0.turicum.memory.LngObject;

/**
 * Implement the len function more or less as it is implemented in Python.
 * Return the length of a string or a length of a list, which you can get anyway using the 'length' field.
 * For any Java collection it will also return the size.
 */
public class ObjectFunction implements TuriFunction {
    @Override
    public String name() {
        return "list";
    }

    @Override
    public java.lang.Object call(Context context, java.lang.Object[] args) throws ExecutionException {
        ExecutionException.when(args.length != 0, "The function 'object()' don't take any arguments");
        return new LngObject(null,((javax0.turicum.memory.Context)context).open());
    }
}
