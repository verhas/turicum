package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;

public class ClosureDefinition extends AbstractCommand {
    public final ParameterList arguments;

    public ParameterList arguments() {
        return arguments;
    }

    public BlockCommand body() {
        return body;
    }

    public ClosureDefinition(ParameterList arguments, BlockCommand body) {
        this.arguments = arguments;
        this.body = body;
    }

    public final BlockCommand body;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        return new Closure(null,arguments, context, body);
    }
}
