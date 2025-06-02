package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

import java.util.ArrayList;
import java.util.HashSet;

@Operator.Symbol("%")
public class Mod extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);
        if( op1 instanceof String s){
            final var parameters = new ArrayList<>();
            if( op2 instanceof Iterable<?> it){
                for( Object o : it){
                    parameters.add(o);
                }
            }else{
                parameters.add(op2);
            }
            return s.formatted(parameters.toArray());
        }

        if (op1 instanceof LngList list1) {
            final var resultList = new LngList();
            if (op2 instanceof LngList list2) {
                final var set2 = new HashSet<>(list2.array);
                for (var elem : list1.array) {
                    if (set2.contains(elem)) {
                        resultList.array.add(elem);
                    }
                }
            } else {
                // If op2 is a single element, intersect is keeping only that element if it exists
                for (var elem : list1.array) {
                    if (elem.equals(op2)) {
                        resultList.array.add(elem);
                    }
                }
            }
            return resultList;
        }

        return binary("mod", op1, op2, (a, b) -> a % b, (a, b) -> a % b);
    }
}
