package javax0.turicum.commands;


import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public record If(Command condition, Command then, Command otherwise) implements Command {
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
