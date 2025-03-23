package javax0.genai.pl.commands;


import javax0.genai.pl.memory.Context;
import javax0.genai.pl.memory.VariableLeftValue;

public record ExportAssignment(VariableLeftValue identifier, Command expression) implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        final var value = expression.execute(ctx);
        ctx.exportLet(identifier.variable, value);
        return value;
    }
}
