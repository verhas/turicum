package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.commands.Command;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

import java.util.HashSet;

@Operator.Symbol("|")
public class Bor extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // for lists calculate unique union
        if (op1 instanceof LngList list1) {
            final var result = new LngList();
            if (op2 instanceof LngList list2) {
                final var set = new HashSet<>();
                for (final var elem : list1.array) {
                    if (!set.contains(elem)) {
                        set.add(elem);
                        result.array.add(elem);
                    }
                }
                for (final var elem : list2.array) {
                    if (!set.contains(elem)) {
                        set.add(elem);
                        result.array.add(elem);
                    }
                }
            } else {
                final var set = new HashSet<>();
                for (final var elem : list1.array) {
                    if (!set.contains(elem)) {
                        set.add(elem);
                        result.array.add(elem);
                    }
                }
                if (!set.contains(op2)) {
                    result.array.add(op2);
                }
            }
            return result;
        }


        return binary("or", op1, op2, (a, b) -> a | b, null);
    }

}
