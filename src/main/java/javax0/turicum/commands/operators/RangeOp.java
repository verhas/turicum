package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExecutionException;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.Range;
import javax0.turicum.memory.Spread;

@Operator.Symbol("..")
public class RangeOp extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        if (left == null) {
            return new Spread(right.execute(ctx));
        }
        final var op1 = left.execute(ctx);
        final var op2 = right.execute(ctx);
        if (Cast.isLong(op1) && Cast.isLong(op2)) {
            return new Range(Cast.toLong(op1), Cast.toLong(op2));
        }
        throw new ExecutionException("Range needs two numbers on the sides of  ':' got %s and %s", op1, op2);
    }

}
