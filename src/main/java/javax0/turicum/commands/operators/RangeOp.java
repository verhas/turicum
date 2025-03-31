package javax0.turicum.commands.operators;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Command;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.Range;
import javax0.turicum.memory.Spread;

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
