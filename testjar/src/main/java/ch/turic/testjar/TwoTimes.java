package ch.turic.testjar;

import ch.turic.Command;
import ch.turic.Context;
import ch.turic.exceptions.ExecutionException;
import ch.turic.TuriMacro;
import ch.turic.builtins.functions.FunUtils;

/**
 * Just a macro that executes the argument twice.
 */
public class TwoTimes implements TuriMacro {
    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var arg = FunUtils.arg(name(),arguments, Command.class);
        final var ctx = FunUtils.ctx(context);
        arg.execute(ctx);
        arg.execute(ctx);
        return null;
    }
}
