package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.VariableLeftValue;

public record ExportAssignment(VariableLeftValue identifier, Command expression) implements Command {
    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();
        final var value = expression.execute(ctx);
        ctx.exportLet(identifier.variable, value);
        return value;
    }
}
