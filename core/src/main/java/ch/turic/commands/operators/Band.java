package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

import java.util.HashSet;

@Operator.Symbol("&")
public class Band extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // for lists calculate unique union
        if (op1 instanceof LngList list1) {
            if (op2 instanceof LngList list2) {
                return mergeLists(list1, list2);
            } else {
                final var result = new LngList();
                if (list1.array.contains(op2)) {
                    result.array.add(op2);
                }
                return result;
            }
        }

        return binary("and", op1, op2, (a, b) -> a & b, null);
    }

    private static LngList mergeLists(LngList list1, LngList list2) {
        final var result = new LngList();
        final var set = new HashSet<>();
        final var set2 = new HashSet<>(list2.array);
        for (final var elem : list1.array) {
            if (!set.contains(elem) && set2.contains(elem)) {
                set.add(elem);
                result.array.add(elem);
            }
        }
        return result;
    }

}

