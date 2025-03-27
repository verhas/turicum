package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExecutionException;
import javax0.turicum.memory.Context;

@Operator.Symbol("%")
public class Mod extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        final var op1 = left.execute(ctx);
        final var op2 = right.execute(ctx);

        return binary("mod", op1, op2, (a, b) -> a % b, (a, b) -> a % b);
    }
}
