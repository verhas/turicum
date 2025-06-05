package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Set the value of a variable in the caller's context.
 */
public class SetCaller implements TuriFunction {
    @Override
    public String name() {
        return "set_caller";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        if( arguments.length != 2 ){
            throw new ExecutionException("'%s' requires 2 arguments", name());
        }
        if( !(context instanceof ch.turic.memory.Context ctx)){
            throw new ExecutionException("'%s' requires the context to be a MemoryContext", name());
        }
        if( !(arguments[0] instanceof String name) ){
            throw new ExecutionException("%s requires a string as first argument, as name of the variable to set. Got '%s'", name(), arguments[0]);
        }
        final var value = arguments[1];
        ctx.caller().update(name,value);
        return value;
    }
}
