package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.memory.LngObject;
import ch.turic.utils.StringUtils;

/**
 * The {@code Env} class implements the {@code TuriFunction} interface, providing
 * a function that retrieves environment variables by their name.
 * <p>
 * This function is registered in the Turi language system using the name "env".
 * It allows querying of environment variables from the underlying operating system
 * by passing the variable name as an argument.
 *
 * <pre>{@code
 * // TUricum is implemented in Java, so this env variable is probably defined
 * die "" when env("JAVA_HOME") == none
 * }</pre>
 */
public class Env implements TuriFunction {

    /**
     * Executes the function call to retrieve the value of an environment variable.
     *
     * @param context   The execution context for the Turi language.
     * @param arguments An array of arguments provided to the function call.
     *                  The first argument is expected to be a {@code String} representing the name of the environment variable.
     * @return The value of the environment variable as a {@code String},
     * or {@code null} if the environment variable does not exist.
     * @throws ExecutionException If there is an error during the execution.
     */
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var ctx = FunUtils.ctx(context);
        final var args = FunUtils.args(name(), arguments, Object[].class);
        if (args.N > 1) {
            throw new ExecutionException("Turic function expects at most one argument");
        }
        if (args.N == 0) {
            final var result = LngObject.newEmpty(ctx);
            for (final var e : System.getenv().entrySet()) {
                final var key = e.getKey();
                final var value = e.getValue();
                result.setField(key, value);
            }
            return result;
        }
        final var varName = args.at(0).as(String.class);
        if (varName.contains("*") || varName.contains("?")) {
            final var result = LngObject.newEmpty(ctx);
            for (final var e : System.getenv().entrySet()) {
                final var key = e.getKey();
                final var value = e.getValue();
                if (StringUtils.matches(varName, key)) {
                    result.setField(key, value);
                }
            }
            return result;

        } else {
            return System.getenv(varName);
        }
    }
}
