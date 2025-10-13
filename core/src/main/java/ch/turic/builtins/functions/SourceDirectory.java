package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0430

end snippet */

/**
 * Returns the the source directory of the currently running code as a string.
 */
public class SourceDirectory implements TuriFunction {

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        FunUtils.noArg(name(), arguments);
        final var ctx = FunUtils.ctx(context);
        final var path = ctx.sourcePath();
        if (path != null) {
            return path.getParent().toFile().getAbsolutePath();
        } else {
            return null;
        }
    }
}
