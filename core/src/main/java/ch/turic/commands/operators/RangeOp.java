package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.memory.Context;
import ch.turic.memory.Range;
import ch.turic.memory.Spread;

@Operator.Symbol("..")
public class RangeOp extends AbstractOperator {

    @Override
    public Object unaryOp(Context ctx, Object op) throws ExecutionException {
        return new Spread(op);
    }

    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        return new Range(op1, right.execute(ctx));
    }

}
