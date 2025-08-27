package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;

import java.util.regex.Pattern;

/**
 * Create a regular expression object.
 *
 * It is recommended to use the {@code turi.re} import and not this built-in function directly.
 */
@Name("_rx")
public class Rx implements TuriFunction {

    @Override
    public Object call(Context ctx, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(), arguments);
        return Pattern.compile(arg.toString());
    }
}
