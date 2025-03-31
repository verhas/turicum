package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

@Operator.Symbol("or")
public class Pipe extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        ExecutionException.when(left == null, "Unary | is not implemented yet");
        final var op1 = left.execute(ctx);
        if (op1 != null) {
            return op1;
        }
        return right.execute(ctx);
    }

}
