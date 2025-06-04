package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;

@Operator.Symbol("**")
public class Pow extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        return binary("pow", op1, op2, Pow::pow, Math::pow);
    }

    private static long pow(final long a, final long b) {
        if (b < 0) throw new ExecutionException("Negative exponents not supported for integers");
        if (b == 0) return 1;
        if (b == 1) return a;

        long result = 1;
        long base = a;
        long exponent = b;

        while (exponent > 0) {
            if ((exponent & 1) == 1) {
                result *= base;
            }
            base *= base;
            exponent >>= 1;
        }
        return result;
    }


}
