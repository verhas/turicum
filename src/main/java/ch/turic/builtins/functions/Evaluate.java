package ch.turic.builtins.functions;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;
import ch.turic.commands.Command;

/**
 * Evaluate a Command
 */
public class Evaluate implements TuriFunction {
    @Override
    public String name() {
        return "evaluate";
    }

    @Override
    public Object call(Context context, Object[] args) throws ExecutionException {
        FunUtils.oneArg(name(),args);
        final var arg = args[0];
        if (arg instanceof Command command) {
            final var ctx = (ch.turic.memory.Context) context;
            final var caller = ctx.caller();
            if( !(caller instanceof ch.turic.memory.Context callerContext)) {
                throw new ExecutionException("caller is not a context");
            }
            return command.execute(callerContext);
        }
        throw new ExecutionException("Cannot evaluate the value of %s", arg);
    }

}
