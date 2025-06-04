package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngObject;

/**
 * Return the identity hash code of an object
 */
public class IdHash implements TuriFunction {
    @Override
    public String name() {
        return "id_hash";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        if (FunUtils.oneArg(name(), args) instanceof LngObject lngObject) {
            return (long) System.identityHashCode(lngObject);
        } else {
            return null;
        }
    }
}
