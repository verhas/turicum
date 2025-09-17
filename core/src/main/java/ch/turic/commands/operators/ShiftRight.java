package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.memory.LocalContext;
import ch.turic.memory.LngList;

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
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s1) {
            final var s2 = op2.toString();
            final var len = Math.max(s1.length(), s2.length());
            final var sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i < s1.length()) {
                    sb.append(s1.charAt(i));
                }
                if (i < s2.length()) {
                    sb.append(s2.charAt(i));
                }
            }
            return sb.toString();
        }
        if (op1 instanceof LngList list1 && op2 instanceof LngList list2) {
            final var len = Math.max(list1.array.size(), list2.array.size());
            final var result = new LngList(list1.getFieldProvider());
            for (int i = 0; i < len; i++) {
                if (i < list1.array.size()) {
                    result.array.add(list1.array.get(i));
                }
                if (i < list2.array.size()) {
                    result.array.add(list2.array.get(i));
                }
            }
            return result;
        }

        return binary("shr", op1, op2, (a, b) -> (a >> b), ShiftRight::shr);
    }

}
