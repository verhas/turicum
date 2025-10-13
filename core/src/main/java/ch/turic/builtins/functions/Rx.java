package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.SnakeNamed.Name;
import ch.turic.TuriFunction;

import java.util.regex.Pattern;
/*snippet builtin0340

end snippet */

/**
 * Create a regular expression object.
 * <p>
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
