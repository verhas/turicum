package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.SnakeNamed;
import ch.turic.TuriFunction;

/**
 * The SetCallerForce class represents a Turi function implementation for updating
 * a value within the caller's execution context even when the caller is pinned.
 * <p>
 * This class implements the TuriFunction interface, making it part of the Turi language
 * system that can be dynamically loaded and executed. The function takes exactly two arguments:
 * a string representing the name and an object representing the value to be set. It operates
 * by updating the caller's internal state with the specified value under the given name.
 * <p>
 * The call method enforces strict validation of arguments to ensure correct usage. Argument
 * validation is performed through utility functions, which verify the argument count
 * and types as required by this function.
 * <p>
 * Key Responsibilities:
 * - Validating the argument count and their types
 * - Extracting and processing the input arguments
 * - Updating the caller's state with the provided name-value pair
 * - Returning the value that was set in the caller's state
 * <p>
 * Errors:
 * - Throws ExecutionException if the argument validation fails or any execution-related error occurs.
 */
@SnakeNamed.Name("set_caller_force")
public class SetCallerForce implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.twoArgs(name(), arguments);
        final var args = FunUtils.args(name(), arguments, String.class, Object.class);
        final var ctx = FunUtils.ctx(context);
        final var name = args.at(0).get().toString();
        final var value = args.at(1).get();
        ctx.caller().updateForce(name, value);
        return value;
    }
}
