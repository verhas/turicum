package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

/**
 * Macro is similar to a closure, but it gets the arguments as {@link Command} objects and not evaluated.
 * After that the body of the macro can decide to evaluate none, one, some or all of the arguments each one or more
 * times as it needs.
 *
 * @param parameters the names of the parameters
 * @param wrapped the wrapped context that was around the closure when defined
 * @param command the block command of the closure
 */
public record Macro(ParameterList parameters, Context wrapped, BlockCommand command) implements HasParametersWrapped {
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
}
