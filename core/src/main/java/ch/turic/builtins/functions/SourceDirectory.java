package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
/*snippet builtin0430

=== `source_directory`

This function returns the directory as a string of the source file or the compiled file that is currently being executed.
It may return `none` if the environment executes a script that lacks a specified file.

It is NOT the script name.
It is not the current working directory.
This is the directory where the Turicum program is located.

end snippet */

/**
 * Returns the source directory of the currently running code as a string.
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
