package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

/**
 * A closure is a block of commands that can get evaluated with arguments.
 *
 * @param parameters the name of the parameters that get string assigned to them before executing the closure
 * @param wrapped the context, possibly null if this closure comes from a 'fn' declaration, that was surrounding the
 *                definition of the closure.
 * @param command the block command that is the body of the closure.
 */
public record Closure(String[] parameters, Context wrapped, BlockCommand command) implements HasParametersWrapped {
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
