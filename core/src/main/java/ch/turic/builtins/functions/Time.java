package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0450

=== `time`

Returns the current time in milliseconds,
the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.

Note that while the unit of time of the return value is a millisecond,
the granularity of the value depends on the underlying operating system and may be larger.
For example, many operating systems measure time in units of tens of milliseconds.

end snippet */

/**
 * the function {@code time()} returns the time in milliseconds as it is returned by the
 * Java {@link System#currentTimeMillis()}.
 */
public class Time implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.noArg(name(), arguments);
        return System.currentTimeMillis();
    }

}
