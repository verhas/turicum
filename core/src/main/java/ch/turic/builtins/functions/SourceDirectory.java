package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * Represents a function implementation in the Turi system that retrieves the
 * source directory path of the currently executing context, if available.
 * <p>
 * This class implements the TuriFunction interface and provides functionality
 * to be registered and executed in the Turi language runtime. It is designed to
 * fetch the parent directory of the source path associated with the given execution
 * context.
 * <p>
 * The `name` method returns the identifier of this function, which is "source_directory".
 * The `call` method performs the main operation of fetching and returning the
 * source directory path, or null if no source path is defined in the context.
 * <p>
 * Throws:
 * - ExecutionException if there are issues during the execution of this function.
 */
public class SourceDirectory implements TuriFunction {
    @Override
    public String name() {
        return "source_directory";
    }

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
