package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;

public class FunctionDefinition extends AbstractCommand {
    public final String functionName;
    public final ParameterList arguments;
    private final TypeDeclaration[] returnType;

    public ParameterList arguments() {
        return arguments;
    }

    public BlockCommand body() {
        return body;
    }

    public FunctionDefinition(String functionName, ParameterList arguments, final TypeDeclaration[] returnType, BlockCommand body) {
        this.arguments = arguments;
        this.functionName = functionName;
        this.returnType = returnType;
        this.body = body;
    }

    public final BlockCommand body;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        final var closure = new Closure(functionName, arguments, null, FunctionCall.calculateTypeNames(context, returnType), body);
        if (functionName != null) {
            context.local(functionName, closure);
        }
        return closure;
    }
}
