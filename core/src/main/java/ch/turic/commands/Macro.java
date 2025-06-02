package ch.turic.commands;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.utils.NullableOptional;

import static ch.turic.commands.FunctionCall.ArgumentEvaluated;

/**
 * Macro is similar to a closure, but it gets the arguments as {@link Command} objects and not evaluated.
 * After that the body of the lazy can decide to evaluate none, one, some or all of the arguments each one or more
 * times as it needs.
 */
public final class Macro extends AbstractCommand implements ClosureOrMacro {
    final ParameterList parameters;
    final Context wrapped;
    final BlockCommand command;
    final String name;

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
    public Context wrapped() {
        return wrapped;
    }

    public Macro(String name, ParameterList parameters, Context wrapped, BlockCommand command) {
        this.parameters = parameters;
        this.wrapped = wrapped;
        this.command = command;
        this.name = name;
    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        Object result = null;
        for (final var command : command.commands()) {
            ExecutionException.when(command instanceof BreakCommand, "You cannot break from a function or closure. Use Return");
            result = command.execute(ctx);
            if (result instanceof Conditional.ReturnResult returnResult && returnResult.isDone()) {
                return returnResult.result();
            }
        }
        return result;
    }

    @Override
    public NullableOptional<Object> methodCall(Context context, HasFields obj, String methodName, FunctionCall.Argument[] arguments) {
        final var argValues = evaluateArguments(context, arguments);
        return ClosureOrMacro.callTheMethod(context, obj, methodName, argValues, this);
    }

    @Override
    public FunctionCall.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCall.Argument[] arguments) {
        return evaluateMacroArguments(context, arguments);
    }
    public static FunctionCall.ArgumentEvaluated[] evaluateMacroArguments(Context context, FunctionCall.Argument[] arguments) {
        final var argValues = arguments == null ? new ArgumentEvaluated[0] : new ArgumentEvaluated[arguments.length];
        for (int i = 0; i < argValues.length; i++) {
            argValues[i] = new ArgumentEvaluated(arguments[i].id(), arguments[i].expression());
        }
        return argValues;
    }
}
