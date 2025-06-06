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

    /**
     * Performs addition or merging based on operand types.
     *
     * If the left operand is a string, concatenates it with the right operand as a string (disallowing control flow values). If the left operand is a list, returns a new list with elements from both operands. If both operands are objects, returns a merged object combining their fields. For all other types, performs numeric addition.
     *
     * @param op1 the left operand
     * @param right the command producing the right operand
     * @return the result of addition, concatenation, or merging, depending on operand types
     * @throws ExecutionException if string concatenation is attempted with a control flow value
     */
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

        // if the left side is an object, then merge the objects
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
