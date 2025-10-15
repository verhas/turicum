package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngObject;
/*snippet builtin0140

=== `is_obj`

This is a function that returns `true` if the argument is an object.

{%S is_obj%}

end snippet */

/**
 * The function {@code is_obj()} returns {@code true} if the argument is an object.
 *
 * <pre>{@code
 * let a = 13
 * let b = "string"
 * let k = { a: a , b: b}
 * die "" when is_obj(a)
 * die "" when is_obj(b)
 * die "" when !is_obj(k)
 *
 * }</pre>
 *
 *
 * The IsObject class implements the TuriFunction interface and provides
 * functionality to determine if a given argument is an instance of LngObject.
 * This function is identified by the name "is_obj" and is used in the context
 * of the Turi language environment.
 * <p>
 * This function expects a single argument to be passed during its execution.
 * If the argument is an instance of LngObject, the function returns true.
 * Otherwise, it returns false.
 * <p>
 * Methods implemented:
 * - name(): Returns the identifier name of the function ("is_obj").
 * - call(Context, Object[]): Executes the logic to check the type of the argument
 * and returns the result.
 * <p>
 * Exceptions:
 * - ExecutionException: Thrown if function execution encounters an error,
 * such as an incorrect number of arguments.
 */
public class IsObj implements TuriFunction {
    @Override
    public String name() {
        return "is_obj";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        return FunUtils.arg(name(), arguments) instanceof LngObject;
    }
}
