package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

import java.util.HashSet;

@Operator.Symbol("^")
public class Xor extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // for lists calculate the symmetric difference
        if (op1 instanceof LngList list1) {
            final var resultList = new LngList();
            if (op2 instanceof LngList list2) {
                final var set1 = new HashSet<>(list1.array);
                final var set2 = new HashSet<>(list2.array);

                // Elements in set1 but not in set2
                for (var elem : list1.array) {
                    if (!set2.contains(elem)) {
                        resultList.array.add(elem);
                    }
                }
                // Elements in set2 but not in set1
                for (var elem : list2.array) {
                    if (!set1.contains(elem)) {
                        resultList.array.add(elem);
                    }
                }
            } else {
                // If op2 is not a list, treat it as a singleton
                if (list1.array.contains(op2)) {
                    // op2 is in list1, so symmetric difference is list1 without op2
                    for (var elem : list1.array) {
                        if (!elem.equals(op2)) {
                            resultList.array.add(elem);
                        }
                    }
                } else {
                    // op2 is not in list1, so add all elements + op2
                    resultList.array.addAll(list1.array);
                    resultList.array.add(op2);
                }
            }
            return resultList;
        }


        return binary("xor", op1, op2, (a, b) -> a ^ b, (a, b) -> {
            throw new ExecutionException("^ is not implemented for floating point numbers");
        });
    }

}
