package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

@Operator.Symbol("%")
public class Mod extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        return binary("mod", op1, op2, (a, b) -> a % b, (a, b) -> a % b);
    }
}
