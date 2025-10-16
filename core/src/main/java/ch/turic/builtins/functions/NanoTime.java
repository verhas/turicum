package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0280

=== `nano_time`

The `nano_time` function in Turicum returns the current value of the most precise available system timer, expressed in nanoseconds.
It is a direct wrapper around Java’s `+System.nanoTime()+` method.
It is typically used for measuring elapsed time with high resolution.

Unlike timestamps, the returned value has no absolute meaning but is strictly monotonic, making it ideal for performance profiling and timing operations.

{%S nano_time%}

end snippet */

/**
 * This function returns the system nano time. It can be used to measure the time passed, and is not suitable for
 * general time use. When calculating the difference between two time points, the time is in nanoseconds.
 *
 * <pre>{@code
 * let t1 = nano_time()
 * let t2 = nano_time()
 * println $"The number of none seconds between two commands was ${(t2-t1)/1_000_000.0}ms"
 * }</pre>
 */
public class NanoTime implements TuriFunction {
    @Override
    public String name() {
        return "nano_time";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.noArg(name(), arguments);
        return System.nanoTime();
    }

}
