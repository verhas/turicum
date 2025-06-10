package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

import java.util.Objects;

/**
 * Represents the implementation of a TuriFunction named "str".
 * This function converts the provided argument to its string representation.
 * If the argument is null, it defaults to the string "none".
 */
public class Str implements TuriFunction {
    @Override
    public String name() {
        return "str";
    }

    /**
     * Executes the function to convert the provided argument to its string representation.
     * If the argument is null, defaults to the string "none".
     *
     * @param ctx       The execution context in which the function is called.
     * @param arguments The arguments passed to the function. The first argument
     *                  is expected to be converted to a string.
     * @return The string representation of the provided argument, or "none" if the argument is null.
     * @throws ExecutionException If an error occurs during the execution of the function.
     */
    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments, Object.class);
        return Objects.requireNonNullElse(arg, "none").toString();
    }
}
