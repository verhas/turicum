package javax0.turicum.commands.operators;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Command;
import javax0.turicum.memory.Context;

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
