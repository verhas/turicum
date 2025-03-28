package javax0.turicum.commands;

import javax0.turicum.memory.Context;

public record Closure(String[] parameters, Context wrapped, BlockCommand command) implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        Object result = null;
        for (final var command : command.commands()) {
            if (command instanceof BreakCommand brk) {
                final var breakResult = brk.execute(ctx);
                if (breakResult.doBreak()) {
                    result = breakResult.result();
                    break;
                }
            } else {
                result = command.execute(ctx);
            }
        }
        return result;

    }
}
