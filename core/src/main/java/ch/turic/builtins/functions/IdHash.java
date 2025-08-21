package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

/**
 * The function {@code id_hash} returns the identity hash of an object.
 * <pre>{@code
 * let z = { a: 13, k: "claus", h: none }
 * // prints out something like 809300666
 * println id_hash(z)
 * }</pre>
 */
public class IdHash implements TuriFunction {

    /****
     * Returns the identity hash code of the provided LngObject argument.
     * <p>
     * If the single argument is an instance of LngObject, its identity hash code is returned as a long. 
     * Returns null if the argument is not a LngObject or if the argument count is incorrect.
     *
     * @param arguments an array containing a single argument to be hashed
     * @return the identity hash code of the LngObject as a long, or null if not applicable
     * @throws ExecutionException if argument validation fails
     */
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        if (FunUtils.arg(name(), arguments) instanceof LngObject lngObject) {
            return (long) System.identityHashCode(lngObject);
        } else if (FunUtils.arg(name(), arguments) instanceof LngList lngList) {
            return (long) System.identityHashCode(lngList);
        } else {
            return null;
        }
    }
}
