package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;
import ch.turic.memory.LngObject;
import ch.turic.memory.Variable;
import ch.turic.utils.NullableOptional;

import java.util.Arrays;
import java.util.SequencedMap;

/**
 * A closure is a block of commands that can get evaluated with arguments.
 * The arguments are evaluated before the code of the closure is executed.
 * <p>
 * Functions and closures use this class. The difference between them is that closures have a wrapped context, and
 * functions do not.
 */
public final class Closure extends AbstractCommand implements ClosureOrMacro, LngCallable.LngCallableClosure {
    final String name;

    final ParameterList parameters;
    final Context wrapped;
    final String[] returnType;
    final BlockCommand command;

    public Closure(String name, ParameterList parameters, Context wrapped, String[] returnType, BlockCommand command) {
        this.name = name;
        this.parameters = parameters;
        this.wrapped = wrapped;
        this.returnType = returnType;
        this.command = command;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ParameterList parameters() {
        return parameters;
    }

    @Override
    public Context wrapped() {
        return wrapped;
    }

    public BlockCommand command() {
        return command;
    }

    public String[] returnType() {
        return returnType;
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
        if (isOfTypes(ctx, result, returnType)) {
            return result;
        }
        throw new ExecutionException(
                "Cannot return from '%s' the value '%s' as it does not fit any of the accepted type of the function/closure (%s)",
                name,
                result,
                String.join(",", returnType));

    }

    public static boolean isOfTypes(final Context ctx, final Object value, String[] types) {
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

    /**
     * A helper function to call a closure directly from Java code.
     *
     * @param callerContext the context of the call
     * @param params        the parameters evaluated. All parameters are processed as positional parameters.
     *                      If the closure has {@code {meta}} parameter it will be empty.
     * @return the result of the closure
     * @throws ExecutionException if there are not enough parameters provided, too many, or the closure throws exception
     */
    @Override
    public Object call(final ch.turic.Context callerContext, final Object... params) {
        if (!(callerContext instanceof Context context)) {
            throw new RuntimeException("Cannot work with this context implementation. This is an internal error.");
        }
        final var ctx = context.wrap(this.wrapped);
        FunctionCall.defineArgumentsInContext(ctx, context, parameters, Arrays.stream(params)
                .map(param -> new FunctionCall.ArgumentEvaluated(null, param)).
                toArray(FunctionCall.ArgumentEvaluated[]::new), true);
        return execute(ctx);
    }

    /**
     * Executes a method call on a provided object in a given context with specified parameters.
     * This method uses positional parameters passed as varargs, where the order of parameters matters
     * for matching with the method's formal parameters. For named parameters, use the overloaded version
     * that accepts a SequencedMap instead.
     *
     * @param context    The execution context in which the method call will be performed.
     * @param obj        The object on which the method will be invoked.
     * @param methodName The name of the method to call.
     * @param params     The parameters to be passed to the method during invocation as positional arguments.
     * @return The result of the method execution.
     * @throws ExecutionException If the method cannot be executed on the provided object or if any execution errors occur.
     * @see #callAsMethod(Context, LngObject, String, SequencedMap)
     */
    public Object callAsMethod(final Context context, final LngObject obj, final String methodName, final Object... params) {
        final var argValues = new FunctionCall.ArgumentEvaluated[params.length];
        for (int i = 0; i < params.length; i++) {
            argValues[i] = new FunctionCall.ArgumentEvaluated(null, params[i]);
        }
        final var res = ClosureOrMacro.callTheMethod(context, obj, methodName, argValues, this);
        if (res.isPresent()) {
            return res.get();
        } else {
            throw new ExecutionException("Calling method %s is not possible on object %s", methodName, obj);
        }

    }

    /**
     * Executes a method call on a provided object in a specified context with the given parameters.
     * This overload differs from {@link #callAsMethod(Context, LngObject, String, Object...)} by accepting
     * named parameters in a SequencedMap instead of positional varargs parameters.
     *
     * @param context    The execution context in which the method call is performed.
     * @param obj        The object on which the specified method will be invoked.
     * @param methodName The name of the method to invoke on the provided object.
     * @param params     A map of parameter names and corresponding values to be passed to the method during invocation.
     *                   SequencedMap is used instead of a regular Map to maintain the order of parameters,
     *                   which is crucial for proper method invocation as parameter position matters for matching
     *                   arguments to their formal parameters in the method signature.
     * @return The result of the executed method on the object.
     * @throws ExecutionException If the method cannot be invoked on the given object or there is an error during execution.
     */
    public Object callAsMethod(final Context context, final LngObject obj, final String methodName, final SequencedMap<String, Object> params) {
        final var argValues = new FunctionCall.ArgumentEvaluated[params.size()];
        final var iterator = params.entrySet().iterator();
        for (int i = 0; i < params.size(); i++) {
            final var entry = iterator.next();
            argValues[i] = new FunctionCall.ArgumentEvaluated(new Identifier(entry.getKey()), entry.getValue());
        }
        final var res = ClosureOrMacro.callTheMethod(context, obj, methodName, argValues, this);
        if (res.isPresent()) {
            return res.get();
        } else {
            throw new ExecutionException("Calling method %s is not possible on object %s", methodName, obj);
        }

    }

    @Override
    public NullableOptional<Object> methodCall(Context context, HasFields obj, String methodName, FunctionCall.Argument[] arguments) {
        final var argValues = evaluateArguments(context, arguments);
        return ClosureOrMacro.callTheMethod(context, obj, methodName, argValues, this);
    }

    @Override
    public FunctionCall.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCall.Argument[] arguments) {
        return evaluateClosureArguments(context, arguments);
    }

    public static FunctionCall.ArgumentEvaluated[] evaluateClosureArguments(Context context, FunctionCall.Argument[] arguments) {
        final var argValues = arguments == null ? new FunctionCall.ArgumentEvaluated[0] : new FunctionCall.ArgumentEvaluated[arguments.length];
        for (int i = 0; i < argValues.length; i++) {
            argValues[i] = new FunctionCall.ArgumentEvaluated(arguments[i].id(), arguments[i].expression().execute(context));
        }
        return argValues;
    }
}
