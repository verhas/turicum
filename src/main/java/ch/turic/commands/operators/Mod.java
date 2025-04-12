package ch.turic.commands.operators;

import ch.turic.commands.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;

@Operator.Symbol("%")
public class Mod extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        return binary("mod", op1, op2, (a, b) -> a % b, (a, b) -> a % b);
    }
}
