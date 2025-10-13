package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0450

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
