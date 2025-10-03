package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngObject;

/**
 * The function {@code pack()} returns an object that encompasses the current local context.
 * Each local variable of the current context will become a field of the returned object.
 * <p>
 * Note that the object contains the current context as its own context and not a copy of it.
 * Defining, modifying, or deleting fields after the object was created will also modify the object.
 */
public class Pack implements TuriFunction {
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        FunUtils.noArg(name(), arguments);
        return new LngObject(null, ctx);
    }
}
