package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;

@Operator.Symbol("**")
public class Pow extends AbstractOperator {

    /**
     * Evaluates the power operation (op1 raised to the result of right) within the given context.
     * <p>
     * Executes the right operand as a command to obtain the exponent, then computes the result using integer or floating-point exponentiation as appropriate.
     *
     * @param ctx the execution context
     * @param op1 the base operand
     * @param right the command representing the exponent
     * @return the result of raising op1 to the power of the evaluated right operand
     * @throws ExecutionException if evaluation fails or the exponent is invalid
     */
    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        return binary("pow", op1, op2, Pow::pow, Math::pow);
    }

    /****
     * Computes the value of a raised to the power of b for non-negative integer exponents using exponentiation by squaring.
     *
     * @param a the base value
     * @param b the exponent value; must be non-negative
     * @return the result of a raised to the power of b
     * @throws ExecutionException if the exponent b is negative
     */
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
