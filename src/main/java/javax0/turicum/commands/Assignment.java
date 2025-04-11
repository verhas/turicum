package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.LeftValue;

public class Assignment extends AbstractCommand {
    final LeftValue leftValue;

    public Command expression() {
        return expression;
    }

    public LeftValue leftValue() {
        return leftValue;
    }

    public Assignment(LeftValue leftValue, Command expression) {
        this.expression = expression;
        this.leftValue = leftValue;
    }

    final Command expression;

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        final var value = expression.execute(ctx);
        leftValue.assign(ctx, value);
        return value;
    }
}
