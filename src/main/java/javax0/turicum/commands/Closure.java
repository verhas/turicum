package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

public record Closure(String[] parameters, Context wrapped, BlockCommand command) implements Command {
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
