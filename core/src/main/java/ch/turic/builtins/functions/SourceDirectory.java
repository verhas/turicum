package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

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
