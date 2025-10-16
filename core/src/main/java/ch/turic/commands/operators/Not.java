package ch.turic.commands.operators;

import ch.turic.exceptions.ExecutionException;
import ch.turic.Command;
import ch.turic.memory.LocalContext;

@Operator.Symbol("!")
public class Not extends AbstractOperator {

    @Override
    public Object unaryOp(LocalContext ctx, Object op) throws ExecutionException {
        ExecutionException.when(!Cast.isBoolean(op), "%s cannot be used as boolean", op);
        return !Cast.toBoolean(op);
    }

    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        throw new ExecutionException( "Somehow '!' is used as a binary operator");

    }
}
