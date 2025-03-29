package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.ExecutionException;
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
        return new Range(left.execute(ctx), right.execute(ctx));
    }

}
