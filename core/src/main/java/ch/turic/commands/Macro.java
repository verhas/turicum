package ch.turic.commands;

import ch.turic.Command;
import ch.turic.memory.Context;
import ch.turic.memory.HasFields;

import static ch.turic.commands.FunctionCallOrCurry.ArgumentEvaluated;

/**
 * Macro is similar to a closure, but it gets the arguments as {@link Command} objects and is not evaluated.
 * After that, the body of the lazy can decide to evaluate none, one, some, or all of the arguments, each one or more
 * times as needed.
 */
public final class Macro extends ClosureOrMacro {

    public Macro(String name, ParameterList parameters, Context wrapped, String[] returnType, BlockCommand command) {
        super(name, parameters, wrapped, returnType, command);
    }

    public Macro(String name, ParameterList parameters, Context wrapped, String[] returnType, BlockCommand command, HasFields curriedSelf, FunctionCallOrCurry.ArgumentEvaluated[] curriedArgs) {
        super(name, parameters, wrapped, returnType, command);
        this.curryThis(curriedSelf, curriedArgs);
    }

    @Override
    public FunctionCallOrCurry.ArgumentEvaluated[] evaluateArguments(Context context, FunctionCallOrCurry.Argument[] arguments) {
        return evaluateMacroArguments(context, arguments);
    }

    /**
     * Evaluates the provided macro arguments in the given context.
     * <p>
     * This method takes an array of arguments and evaluates each of them by converting them
     * into {@link FunctionCallOrCurry.ArgumentEvaluated} objects using their identifiers
     * and expressions. If the {@code arguments} array is null, an empty array is returned, and not {@code null}.
     * <p>
     * This method does not execute the expressions as macros get their argument unevaluated.
     *
     * @param context   the execution context in which the arguments are evaluated
     * @param arguments the array of arguments to be evaluated; can be null
     * @return an array of evaluated arguments in the form of {@link FunctionCallOrCurry.ArgumentEvaluated}
     */
    public static FunctionCallOrCurry.ArgumentEvaluated[] evaluateMacroArguments(Context context, FunctionCallOrCurry.Argument[] arguments) {
        final var argValues = arguments == null ? new ArgumentEvaluated[0] : new ArgumentEvaluated[arguments.length];
        for (int i = 0; i < argValues.length; i++) {
            argValues[i] = new ArgumentEvaluated(arguments[i].id(), arguments[i].expression());
        }
        return argValues;
    }
}
