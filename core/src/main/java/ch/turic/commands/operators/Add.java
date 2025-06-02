package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.commands.Conditional;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

@Operator.Symbol("+")
public class Add extends AbstractOperator {

    /**
     * you can write '+' in front of anything, like +"string" or even an object, that is just the same
     */
    @Override
    public Object unaryOp(Context ctx, Object op) throws ExecutionException {
        return op;
    }

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            ExecutionException.when(op2 instanceof Conditional, "Cannot append break or return value to a string.");
            return s.toString() + op2;
        }

        // if the left side is a list, then merge the lists or append the right side to the list
        if (op1 instanceof LngList list1) {
            final var joinedList = new LngList();
            joinedList.array.addAll(list1.array);
            if (op2 instanceof LngList list2) {
                joinedList.array.addAll(list2.array);
            } else {
                joinedList.array.add(op2);
            }
            return joinedList;
        }

        // if the left side is a list, then merge the lists or append the right side to the list
        if (op1 instanceof LngObject obj1 && op2 instanceof LngObject obj2) {
            final var merged = new LngObject(obj1.lngClass(), ctx.open());
            for (final var f : obj1.fields()) {
                    if (f.equals("this")) {
                        merged.setField(f, merged);
                    } else {
                        merged.setField(f, obj1.getField(f));
                    }
            }
            for (final var f : obj2.fields()) {
                if (!f.equals("cls") && !f.equals("this")) {
                    merged.setField(f, obj2.getField(f));
                }
            }
            return merged;
        }

        return binary("add", op1, op2, Long::sum, Double::sum);
    }

}
