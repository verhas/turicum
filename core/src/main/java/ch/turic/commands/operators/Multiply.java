package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.commands.Command;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

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

        if (op1 instanceof LngList list1 && op2 instanceof LngList list2) {
            final var product = new LngList();
            for (final var a : list1.array) {
                for (final var b : list2.array) {
                    final var pair = new LngList();
                    pair.array.add(a);
                    pair.array.add(b);
                    product.array.add(pair);
                }
            }
            return product;
        }

        return binary("multiply", op1, op2, (a, b) -> a * b, (a, b) -> a * b);
    }

}
