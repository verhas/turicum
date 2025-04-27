package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Set the value of a variable.
 */
public class Set implements TuriFunction {
    @Override
    public String name() {
        return "set";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        if( args.length != 2 ){
            throw new ExecutionException("'%s' requires 2 arguments", name());
        }
        if( !(context instanceof ch.turic.memory.Context ctx)){
            throw new ExecutionException("'%s' requires the context to be a MemoryContext", name());
        }
        if( !(args[0] instanceof String name) ){
            throw new ExecutionException("%s requires a string as first argument, as name of the variable to set. Got '%s'", name(), args[0]);
        }
        final var value = args[1];
        ctx.update(name,value);
        return value;
    }
}
