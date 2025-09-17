package ch.turic.commands;

import ch.turic.Context;
import ch.turic.ExecutionException;
import ch.turic.LngCallable;
import ch.turic.memory.LocalContext;
import ch.turic.memory.LngObject;
import ch.turic.utils.Unmarshaller;

import java.util.Arrays;

/**
 * A closure is a block of commands that can get evaluated with arguments.
 * The arguments are evaluated before the code of the closure is executed.
 * <p>
 * Functions and closures use this class. The difference between them is that closures have a wrapped context, and
 * functions do not.
 */
public final class Closure extends ClosureOrMacro implements LngCallable.LngCallableClosure {

    public Closure(String name, ParameterList parameters, LocalContext wrapped, String[] returnType, BlockCommand command) {
        super(name, parameters, wrapped, returnType, command);
    }

    public static Closure factory(final Unmarshaller.Args args) {
        return new Closure(
                args.str("name"),
                args.get("parameters", ParameterList.class),
                args.get("wrapped", LocalContext.class),
                args.get("returnType", String[].class),
                args.get("command", BlockCommand.class)
        );
    }

    /**
     * A helper function to call a closure directly from Java code.
     *
     * @param callerContext the context of the call
     * @param arguments     the parameters evaluated. All parameters are processed as positional parameters.
     *                      If the closure has {@code {meta}} parameter it will be empty.
     * @return the result of the closure
     * @throws ExecutionException if there are not enough parameters provided, too many, or the closure throws an exception
     */
    @Override
    public Object call(final Context callerContext, final Object... arguments) {
        if (!(callerContext instanceof LocalContext context)) {
            throw new RuntimeException("Cannot work with this context implementation. This is an internal error.");
        }
        final var ctx = context.wrap(this.wrapped);
        FunctionCallOrCurry.defineArgumentsInContext(ctx, context, parameters, Arrays.stream(arguments)
                .map(param -> new FunctionCallOrCurry.ArgumentEvaluated(null, param)).
                toArray(FunctionCallOrCurry.ArgumentEvaluated[]::new), true);
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
     */
    public Object callAsMethod(final LocalContext context, final LngObject obj, final String methodName, final Object... params) {
        final var argValues = new FunctionCallOrCurry.ArgumentEvaluated[params.length];
        for (int i = 0; i < params.length; i++) {
            argValues[i] = new FunctionCallOrCurry.ArgumentEvaluated(null, params[i]);
        }
        final var res = ClosureLike.callTheMethod(context, obj, methodName, argValues, this);
        if (res.isPresent()) {
            return res.get();
        } else {
            throw new ExecutionException("Calling method %s is not possible on object %s", methodName, obj);
        }

    }

    @Override
    public FunctionCallOrCurry.ArgumentEvaluated[] evaluateArguments(LocalContext context, FunctionCallOrCurry.Argument[] arguments) {
        return evaluateClosureArguments(context, arguments);
    }

    /**
     * Evaluates closure arguments within the given context and returns an array of evaluated arguments.
     * Each argument is evaluated by executing its associated expression in the specified context.
     *
     * @param context   the execution context in which the arguments are evaluated
     * @param arguments an array of arguments to be evaluated; if null, an empty array is returned
     * @return an array of {@code FunctionCallOrCurry.ArgumentEvaluated} objects containing the evaluated arguments
     */
    public static FunctionCallOrCurry.ArgumentEvaluated[] evaluateClosureArguments(LocalContext context, FunctionCallOrCurry.Argument[] arguments) {
        final var argValues = arguments == null ? new FunctionCallOrCurry.ArgumentEvaluated[0] : new FunctionCallOrCurry.ArgumentEvaluated[arguments.length];
        for (int i = 0; i < argValues.length; i++) {
            argValues[i] = new FunctionCallOrCurry.ArgumentEvaluated(arguments[i].id(), arguments[i].expression().execute(context));
        }
        return argValues;
    }
}
