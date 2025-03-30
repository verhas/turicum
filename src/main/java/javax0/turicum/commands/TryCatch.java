package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LngException;

public record TryCatch(Command tryBlock, Command catchBlock, Command finallyBlock,
                       String exceptionVariable) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
        final var ctx = context.wrap();
        try {
            tryBlock.execute(ctx);
        } catch (ExecutionException e) {
            final var exception = new LngException(e);
            ctx.let0(exceptionVariable, exception);
            catchBlock.execute(ctx);
        } finally {
            if (finallyBlock != null) {
                catchBlock.execute(ctx);
            }
        }
        return null;
    }
}
