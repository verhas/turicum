package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.operators.Cast;
/*snippet builtin0420

=== `sleep`

Sleep pauses the execution.

`sleep(N)` will sleep approximately `N` seconds.
`N` is an integer or a floating point value.
The sleep time does not need to be whole seconds, but the precision is not finer than 0.001 seconds.

The return value is the actual number of seconds spent with millisecond precision.
It is the same as the argument or a bit more.

{%S sleep%}

This function is implemented by calling the Java JDK method `Thread.sleep()`.
If the specified time is negative, the function does not call the JDK sleep method.
In all other cases, a `Thread.sleep()` call is invoked, with all multi-thread synchronization effects.

end snippet */

/**
 * Sleep pauses the execution.
 * <p>
 * {@code sleep(N)} will sleep approximately {@code N} seconds.
 * It can be a floating point value, does not need to sleep round seconds, but the precision is not finer than
 * 0.001 seconds.
 *
 * The return value is the actual number of seconds spent with millisecond precision.
 */
public class Sleep implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        final long start = System.currentTimeMillis();
        final long waitTime;
        if (Cast.isLong(arg)) {
            waitTime = Cast.toLong(arg) * 1000;
        } else if (Cast.isDouble(arg)) {
            waitTime = Cast.toLong(Cast.toDouble(arg) * 1000);
        } else {
            throw new ExecutionException("cannot sleep for '%s' seconds. What is that?", arg);
        }
        if (waitTime >= 0) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                throw new ExecutionException("Interrupted '%s'", e.getCause());
            }
        }
        final long end = System.currentTimeMillis();
        return (double) (end - start) / 1000.0;
    }

}
