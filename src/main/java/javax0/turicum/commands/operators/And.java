package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExecutionException;
import javax0.turicum.memory.Context;

@Operator.Symbol("&&")
public class And extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        final var op1 = left.execute(ctx);
        ExecutionException.when(!Cast.isBoolean(op1), "%s cannot be used as boolean", op1);
        if (Cast.toBoolean(op1)) {
            final var op2 = right.execute(ctx);
            ExecutionException.when(!Cast.isBoolean(op1), "%s cannot be used as boolean", op1);
            return Cast.toBoolean(op2);
        }
        return false;
    }

}
