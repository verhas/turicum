package javax0.genai.pl.commands.operators;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.memory.Context;

@Operator.Symbol("*")
public class Multiply extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        final var op1 = left.execute(ctx);
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
