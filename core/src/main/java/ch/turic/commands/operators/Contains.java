package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.memory.Context;

import java.util.List;
import java.util.Set;

@Operator.Symbol("in")
public class Contains extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

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
