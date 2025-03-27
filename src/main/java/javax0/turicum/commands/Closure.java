package javax0.turicum.commands;

import javax0.turicum.memory.Context;

public record Closure(String[] parameters, Context wrapped, BlockCommand command) implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        Object result = null;
        final var blockContext = ctx.wrap();
        for (final var command : command.commands()) {
            result = command.execute(blockContext);
        }
        return result;

    }
}
