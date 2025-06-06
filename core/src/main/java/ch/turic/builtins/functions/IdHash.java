package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngObject;

/**
 * Represents a Turi language function that computes the identity hash code of a given argument.
 * The function is called "id_hash" and is part of the TuriFunction interface, ensuring
 * it can be loaded, registered, and executed within the Turi language environment.
 * <p>
 * Key Behavior:
 * <ul>
 * <li> The function expects exactly one argument passed to it.
 * <li> If the argument is an instance of LngObject, the function calculates and returns
 * the identity hash code of the argument using {@link System#identityHashCode(Object)}.
 * <li> If the argument does not meet the expected type or null is provided, the function
 * returns null.
 * This is interpreted in the Turicum language as {@code none}, i.e. the value does not have an identity hash code.
 * </ul>
 * <p>
 * Methods:
 * <ul>
 * <li> name(): Returns the identifier of the function, "id_hash".
 * <li> call(Context, Object[]): Executes the function with the given context and arguments,
 * applying the identity hash code operation to a valid LngObject.
 * </ul>
 * <p>
 * Exceptions:
 * <li> Throws an {@link ExecutionException} if the arguments do not meet the requirements,
 * e.g., if the number of arguments is incorrect.
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
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        if (FunUtils.arg(name(), arguments) instanceof LngObject lngObject) {
            return (long) System.identityHashCode(lngObject);
        } else {
            return null;
        }
    }
}
