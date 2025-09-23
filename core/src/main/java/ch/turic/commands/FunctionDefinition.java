package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

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

    public static FunctionDefinition factory(final Unmarshaller.Args args) {
        return new FunctionDefinition(
                args.str("functionName"),
                args.get("arguments", ParameterList.class),
                args.get("returnType", TypeDeclaration[].class),
                args.get("body", BlockCommand.class)
        );
    }

    public FunctionDefinition(String functionName, ParameterList arguments, final TypeDeclaration[] returnType, BlockCommand body) {
        this.arguments = arguments;
        this.functionName = functionName;
        this.returnType = returnType;
        this.body = body;
    }

    public final BlockCommand body;

    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
        final var closure = new Closure(functionName, arguments, null, FunctionCall.calculateTypeNames(context, returnType), body);
        if (functionName != null) {
            context.local(functionName, closure);
            context.freeze(functionName);
        }
        return closure;
    }
}
