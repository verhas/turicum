package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.commands.Command;
import ch.turic.memory.Context;

@Operator.Symbol("or")
public class Pipe extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        if (op1 != null) {
            return op1;
        }
        return right.execute(ctx);
    }

}
