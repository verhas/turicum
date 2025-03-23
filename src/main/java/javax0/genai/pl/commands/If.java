package javax0.genai.pl.commands;


import javax0.genai.pl.commands.operators.Cast;
import javax0.genai.pl.memory.Context;

public record If(Command condition, BlockCommand then, BlockCommand otherwise) implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        if (Cast.toBoolean(condition.execute(ctx))) {
            return then.execute(ctx);
        } else {
            if (otherwise != null) {
                return otherwise.execute(ctx);
            } else {
                return null;
            }
        }
    }
}
