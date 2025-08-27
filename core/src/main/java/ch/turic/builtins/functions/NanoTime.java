package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * This function returns the system nano time. It can be used to measure the time passed, and is not suitable for
 * general time use. When calculating the difference between two time points, the time is in nanoseconds.
 *
 * <pre>{@code
 * let t1 = nano_time()
 * let t2 = nano_time()
 * println $"The number of none seconds between two commands was ${(t2-t1)/1000_000.0}ms"
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
