package ch.turic.commands.operators;

import ch.turic.exceptions.ExecutionException;
import ch.turic.Command;
import ch.turic.memory.LocalContext;
import ch.turic.utils.BinUtils;

import java.util.List;
import java.util.Set;

@Operator.Symbol("in")
public class Contains extends AbstractOperator {

    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // a bin contains a byte value or a contiguous byte subsequence
        if (op2 instanceof byte[] haystack) {
            if (op1 instanceof byte[] needle) {
                return BinUtils.indexOf(haystack, needle, 0) >= 0;
            }
            if (Cast.isLong(op1)) {
                final var needle = BinUtils.toByte(op1);
                for (final byte b : haystack) {
                    if (b == needle) {
                        return true;
                    }
                }
                return false;
            }
            throw new ExecutionException("Only a byte value or a bin can be sought in a bin, got '%s'", op1);
        }

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            return op2.toString().contains(s);
        }
        if (op2 instanceof Iterable<?> iterable) {
            // we could iterate, like in the default case, but it is a bit optimized
            return switch (iterable) {
                case Set<?> s -> s.contains(op1);
                case List<?> l -> l.contains(op1);
                default -> {
                    for (final var i : iterable) {
                        if (op1.equals(i)) {
                            yield true;
                        }
                    }
                    yield false;
                }
            };
        }
        // note that the op1 and op2 are reversed for op2.contains(op1)
        return binary("contains", op2, op1, null, null);
    }

}
