package javax0.turicum.commands.operators;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Command;
import javax0.turicum.memory.Context;

@Operator.Symbol("!")
public class Not extends AbstractOperator {

    @Override
    public Object unaryOp(Context ctx, Object op) throws ExecutionException {
        ExecutionException.when(!Cast.isBoolean(op), "%s cannot be used as boolean", op);
        return !Cast.toBoolean(op);
    }

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        throw new ExecutionException( "Somehow '!' is used as a binary operator");

    }
}
