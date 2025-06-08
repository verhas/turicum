package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * The {@code Env} class implements the {@code TuriFunction} interface, providing
 * a function that retrieves environment variables by their name.
 * <p>
 * This function is registered in the Turi language system using the name "env".
 * It allows querying of environment variables from the underlying operating
 * system by passing the variable name as an argument.
 */
public class Env implements TuriFunction {
    /**
     * Returns the name of this function, "env".
     *
     * @return the string "env"
     */
    @Override
    public String name() {
        return "env";
    }

    /**
     * Executes the function call to retrieve the value of an environment variable.
     *
     * @param context   The execution context for the Turi language.
     * @param arguments An array of arguments provided to the function call.
     *                  The first argument is expected to be a {@code String} representing the name of the environment variable.
     * @return The value of the environment variable as a {@code String},
     *         or {@code null} if the environment variable does not exist.
     * @throws ExecutionException If there is an error during the execution.
     */
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        if (FunUtils.arg(name(), arguments, String.class) instanceof String env) {
            return System.getenv(env);
        }else{
            throw new ExecutionException(String.format("%s is not a string", name()));
        }
    }
}
