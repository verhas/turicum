package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.memory.HasFields;
import ch.turic.memory.Variable;
import ch.turic.utils.NullableOptional;

public sealed abstract class ClosureOrMacro extends ClosureLike permits Closure, Macro {
    final String name;
    final ParameterList parameters;
    final LocalContext wrapped;
    final String[] returnType;
    final BlockCommand command;
    @Override
    public String name() {
        return name;
    }

    public BlockCommand command() {
        return command;
    }

    @Override
    public ParameterList parameters() {
        return parameters;
    }

    @Override
    public LocalContext wrapped() {
        return wrapped;
    }

    public String[] returnType() {
        return returnType;
    }

    public ClosureOrMacro(String name, ParameterList parameters, LocalContext wrapped, String[] returnType, BlockCommand command) {
        this.name = name;
        this.parameters = parameters;
        this.wrapped = wrapped;
        this.returnType = returnType;
        this.command = command;
    }

    @Override
    public Object _execute(final LocalContext ctx) throws ExecutionException {
        ctx.step();
        Object result = null;
        for (final var cmd : command.commands()) {
            ExecutionException.when(cmd instanceof BreakCommand, "You cannot break from a function or closure. Use Return");
            result = cmd.execute(ctx);
            if (result instanceof Conditional.ReturnResult returnResult && returnResult.isDone()) {
                return returnResult.result();
            }
        }
        if (isOfTypes(ctx, result, returnType)) {
            return result;
        }
        throw new ExecutionException(
                "Cannot return from '%s' the value '%s' as it does not fit any of the accepted type of the macro (%s)",
                name,
                result,
                String.join(",", returnType));
    }

    private static boolean isOfTypes(final LocalContext ctx, final Object value, String[] types) {
        if (types == null || types.length == 0) {
            return true;
        } else {
            for (final var typeName : types) {
                final var type = Variable.getTypeFromName(ctx, typeName);
                if (Variable.isFit(value, type.javaType(), type.lngClass())) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public NullableOptional<Object> methodCall(LocalContext context, HasFields obj, String methodName, FunctionCallOrCurry.Argument[] arguments) {
        final var argValues = evaluateArguments(context, arguments);
        return ClosureLike.callTheMethod(context, obj, methodName, argValues, this);
    }
}
