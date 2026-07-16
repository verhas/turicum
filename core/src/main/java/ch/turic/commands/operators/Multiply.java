package ch.turic.commands.operators;

import ch.turic.exceptions.ExecutionException;
import ch.turic.Command;
import ch.turic.memory.LocalContext;
import ch.turic.memory.LngList;

@Operator.Symbol("*")
public class Multiply extends AbstractOperator {

    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
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

        // a bin is repeated like a string
        if (op1 instanceof byte[] b) {
            if (Cast.isLong(op2)) {
                final var n = Cast.toLong(op2);
                if (n < 0 || n * b.length > Integer.MAX_VALUE) {
                    throw new ExecutionException("Cannot repeat a bin value %s times", n);
                }
                final var repeated = new byte[(int) (n * b.length)];
                for (int i = 0; i < n; i++) {
                    System.arraycopy(b, 0, repeated, i * b.length, b.length);
                }
                return repeated;
            }
            throw new ExecutionException("Cannot '%s' * '%s'", op1, op2);
        }

        if (op1 instanceof LngList list1 && op2 instanceof LngList list2) {
            final var product = new LngList(list1.getFieldProvider());
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
