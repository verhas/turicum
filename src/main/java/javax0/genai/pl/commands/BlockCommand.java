package javax0.genai.pl.commands;


import javax0.genai.pl.memory.Context;

import java.util.List;

public record BlockCommand(List<Command> commands, boolean wrap) implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        Object result = null;
        if (wrap) {
            final var blockContext = ctx.wrap();
            for (final var command : commands) {
                result = command.execute(blockContext);
            }

        } else {
            for (final var command : commands) {
                result = command.execute(ctx);
            }
        }
        return result;
    }
}

