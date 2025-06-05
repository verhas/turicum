package ch.turic.builtins.functions;

import ch.turic.Command;
import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.TuriFunction;

/**
 * The Evaluate class implements the TuriFunction interface and provides the functionality
 * for evaluating a single argument if it is an instance of the Command interface.
 * This class is designed to operate within the Turi language execution context and
 * ensures that the evaluation is performed only in supported environments.
 */
public class Evaluate implements TuriFunction {
    @Override
    public String name() {
        return "evaluate";
    }

    @Override
    public Object call(Context context, Object[] arguments) throws ExecutionException {
        final var command = FunUtils.arg(name(), arguments, Command.class);
        final var caller = FunUtils.ctx(context).caller();
        if (caller instanceof ch.turic.memory.Context callerContext) {
            return command.execute(callerContext);
        } else {
            throw new ExecutionException("'%s' is used outside of macro", name());
        }
    }

}
