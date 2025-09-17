package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.LocalContext;

@Operator.Symbol("||")
public class Or extends AbstractOperator {

    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        ExecutionException.when(!Cast.isBoolean(op1), "%s cannot be used as boolean", op1);
        if (Cast.toBoolean(op1)) {
            return true;
        } else {
            final var op2 = right.execute(ctx);
            ExecutionException.when(!Cast.isBoolean(op1), "%s cannot be used as boolean", op1);
            return Cast.toBoolean(op2);
        }
    }

}
