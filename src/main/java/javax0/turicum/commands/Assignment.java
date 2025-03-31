package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LeftValue;

public record Assignment(LeftValue leftValue, Command expression) implements Command {

    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        final var value = expression.execute(ctx);
        leftValue.assign(ctx, value);
        return value;
    }

}
