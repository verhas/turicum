package javax0.genai.pl.commands;


import javax0.genai.pl.memory.Context;
import javax0.genai.pl.memory.LeftValue;

public record Assignment(LeftValue leftValue, Command expression) implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        final var value = expression.execute(ctx);
        leftValue.assign(ctx, value);
        return value;
    }
}
