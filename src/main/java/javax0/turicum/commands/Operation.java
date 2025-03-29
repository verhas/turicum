package javax0.turicum.commands;


import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Operator;
import javax0.turicum.memory.Context;

import java.util.Objects;

public record Operation(String operator, Command left, Command right) implements Command {

    public Operation {
        Objects.requireNonNull(operator);
        Objects.requireNonNull(right);
    }

    @Override
    public Object execute(final Context ctx) throws ExecutionException {
        ctx.step();

        if (Operator.OPERATORS.containsKey(operator)) {
            Operator op = Operator.OPERATORS.get(operator);
            return op.execute(ctx, left, right);
        }
        throw new ExecutionException("Unknown operator " + operator);
    }

}
