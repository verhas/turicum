package javax0.turicum.commands.operators;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Command;
import javax0.turicum.commands.Conditional;
import javax0.turicum.memory.Context;

@Operator.Symbol(">>")
public class ShiftRight extends AbstractOperator {

    private static double shr(double value, double shiftAmount) {
        long shift = Cast.toLong(shiftAmount);
        if (shift < 0) {
            throw new ExecutionException("Shift amount must be non-negative");
        }

        double factor = 1.0;
        double powerOfTwo = 2.0;

        while (shift != 0) {
            if ((shift & 1) != 0) {
                factor *= powerOfTwo;
            }
            powerOfTwo *= powerOfTwo;
            shift >>= 1;
        }

        return value / factor;
    }

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            ExecutionException.when(op2 instanceof Conditional, "Cannot append break or return value to a string.");
            return s.toString() + op2;
        }

        return binary("shr", op1, op2, (a, b) -> (a >> b), ShiftRight::shr);
    }

}
