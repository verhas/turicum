package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngObject;

/**
 * Return the identity hash code of an object
 */
public class IdHash implements TuriFunction {
    /**
     * Returns the name of this function, "id_hash".
     *
     * @return the string "id_hash"
     */
    @Override
    public String name() {
        return "id_hash";
    }

    /****
     * Returns the identity hash code of the provided LngObject argument.
     *
     * If the single argument is an instance of LngObject, its identity hash code is returned as a long. 
     * Returns null if the argument is not a LngObject or if the argument count is incorrect.
     *
     * @param args an array containing a single argument to be hashed
     * @return the identity hash code of the LngObject as a long, or null if not applicable
     * @throws ExecutionException if argument validation fails
     */
    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        if (FunUtils.oneArg(name(), args) instanceof LngObject lngObject) {
            return (long) System.identityHashCode(lngObject);
        } else {
            return null;
        }
    }
}
