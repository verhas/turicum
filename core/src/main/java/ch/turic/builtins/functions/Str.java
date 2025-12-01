package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;

import java.util.Objects;
/*snippet builtin0440

=== `str`

Converts the argument to a string.

Similar to the `to_string()` method, but it works safely on any value, and in some situations, it may be more readable.
This function converts the value `none` to `pass:["none"]`.
end snippet */

/**
 * Represents the implementation of a TuriFunction named "str".
 * This function converts the provided argument to its string representation.
 * If the argument is null, it defaults to the string "none".
 */
public class Str implements TuriFunction {

    /**
     * Executes the function to convert the provided argument to its string representation.
     * If the argument is null, it defaults to the string "none".
     *
     * @param ctx       The execution context in which the function is called.
     * @param arguments The arguments passed to the function. The first argument
     *                  is expected to be converted to a string.
     * @return The string representation of the provided argument, or "none" if the argument is {@code null}, and also
     * "none" when the original {@link Object#toString() toString()} method returns null.
     * @throws ExecutionException If an error occurs during the execution of the function.
     */
    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments, Object.class);
        return Objects.requireNonNullElse(Objects.requireNonNullElse(arg, "none").toString(),"none");
    }
}
