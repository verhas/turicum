package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

public class FunctionDefinition extends AbstractCommand {
    public final Identifier functionName;
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
                args.get("functionName",Identifier.class),
                args.get("arguments", ParameterList.class),
                args.get("returnType", TypeDeclaration[].class),
                args.get("body", BlockCommand.class)
        );
    }

    public FunctionDefinition(Identifier functionName, ParameterList arguments, final TypeDeclaration[] returnType, BlockCommand body) {
        this.arguments = arguments;
        this.functionName = functionName;
        this.returnType = returnType;
        this.body = body;
    }

    public final BlockCommand body;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        final var fn = functionName == null ? null : functionName.name(context);
        final var args = arguments.bind(context);
        final var closure = new Closure(fn, args, null, FunctionCall.calculateTypeNames(context, returnType), body);
        if (fn != null) {
            context.local(fn, closure);
        }
        return closure;
    }
}
