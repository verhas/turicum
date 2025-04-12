package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;

public class FunctionDefinition extends AbstractCommand {
    public final String functionName;
    public final ParameterList arguments;

    public ParameterList arguments() {
        return arguments;
    }

    public BlockCommand body() {
        return body;
    }

    public String functionName() {
        return functionName;
    }

    public FunctionDefinition(String functionName, ParameterList arguments , BlockCommand body) {
        this.arguments = arguments;
        this.functionName = functionName;
        this.body = body;
    }

    public final BlockCommand body;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        final var closure = new Closure(arguments, null, body);
        if (functionName != null) {
            context.local(functionName, closure);
        }
        return closure;
    }
}
