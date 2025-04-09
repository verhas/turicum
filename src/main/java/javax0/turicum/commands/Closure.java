package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.LngCallable;
import javax0.turicum.memory.Context;

import java.util.Arrays;

/**
 * A closure is a block of commands that can get evaluated with arguments.
 *
 * @param parameters the name of the parameters that get string assigned to them before executing the closure
 * @param wrapped    the context, possibly null if this closure comes from a 'fn' declaration, that was surrounding the
 *                   definition of the closure.
 * @param command    the block command that is the body of the closure.
 */
public record Closure(ParameterList parameters, Context wrapped,
                      BlockCommand command) implements ClosureOrMacro, LngCallable {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
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
    public Object call(final javax0.turicum.Context callerContext, final Object... params) {
        if (!(callerContext instanceof Context context)) {
            throw new RuntimeException("Cannot work with this context implementation. This is an internal error.");
        }
        final var ctx = context.wrap();
        FunctionCall.defineArgumentsInContext(ctx, parameters, Arrays.stream(params)
                .map(param -> new FunctionCall.ArgumentEvaluated(null, param)).
                toArray(FunctionCall.ArgumentEvaluated[]::new));
        return execute(ctx);
    }
}
