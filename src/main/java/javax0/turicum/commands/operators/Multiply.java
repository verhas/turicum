package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

@Operator.Symbol("*")
public class Multiply extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            if (Cast.isLong(op2)) {
                final var n = Cast.toLong(op2);
                if (n > Integer.MAX_VALUE || n < Integer.MIN_VALUE) {
                    throw new ExecutionException("Cannot '%s' * '%s' too large", op2, n);
                }
                return s.toString().repeat(n.intValue());
            }
            throw new ExecutionException("Cannot '%s' * '%s'", op2, op1);
        }
        return binary("multiply", op1, op2, (a, b) -> a * b, (a, b) -> a * b);
    }

}
