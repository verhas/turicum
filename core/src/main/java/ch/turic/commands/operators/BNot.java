package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.commands.Command;
import ch.turic.memory.Context;

@Operator.Symbol("~")
public class BNot extends AbstractOperator {

    @Override
    public Object unaryOp(Context ctx, Object op) throws ExecutionException {
        ExecutionException.when(!Cast.isLong(op), "%s cannot be used as number", op);
        return ~Cast.toLong(op);
    }

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        throw new ExecutionException( "Somehow '~' is used as a binary operator");

    }
}
