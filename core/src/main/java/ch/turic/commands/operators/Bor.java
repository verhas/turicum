package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;
import ch.turic.memory.LngObject;

import java.util.HashSet;

@Operator.Symbol("|")
public class Bor extends AbstractOperator {

    /**
     * Performs a bitwise OR operation or merges operands based on their types.
     *
     * If both operands are lists, returns a new list containing the unique union of their elements.
     * If both operands are objects, returns a new object with fields merged recursively.
     * Otherwise, performs a bitwise OR operation on the operands.
     *
     * @param op1 the left operand, which may be a list, object, or numeric value
     * @param right the command representing the right operand
     * @return the result of the bitwise OR, unique list union, or merged object
     * @throws ExecutionException if an error occurs during command execution
     */
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
        if (op1 instanceof LngObject obj1 && op2 instanceof LngObject obj2) {
            return mergeObjects(obj1, obj2, ctx);
        }
        return binary("or", op1, op2, (a, b) -> a | b, null);
    }

    /**
     * Recursively merges two {@link LngObject} instances into a new object.
     *
     * Fields from the first object are copied to the result. Fields from the second object are added if not present, or merged recursively if both are {@link LngObject}s, concatenated if both are {@link LngList}s, or overwritten otherwise. The resulting object uses the class of the first operand and a new context scope.
     *
     * @param a the base object to merge from
     * @param b the object whose fields are merged into the base
     * @param ctx the execution context for creating the merged object
     * @return a new {@link LngObject} containing merged fields from both input objects
     */
    private static LngObject mergeObjects(final LngObject a, final LngObject b, final Context ctx) {
        final var merged = new LngObject(a.lngClass(), ctx.open());
        // copy all fields from 'a' to the merged object
        for (final var f : a.fields()) {
            if (f.equals("this")) {
                merged.setField(f, merged);
            } else {
                merged.setField(f, a.getField(f));
            }
        }

        // merge the fields from 'b' recursively
        for (final var f : b.fields()) {
            if (!f.equals("cls") && !f.equals("this")) {
                final var aField = merged.getField(f);
                if (aField == null) {
                    merged.setField(f, b.getField(f));
                } else {
                    final var bField = b.getField(f);
                    if (aField instanceof LngObject aObj && bField instanceof LngObject bObj) {
                        merged.setField(f, mergeObjects(aObj, bObj, ctx));
                    } else if (aField instanceof LngList aList && bField instanceof LngList bList) {
                        final var list = new LngList();
                        list.addAll(aList.array);
                        list.addAll(bList.array);
                        merged.setField(f,list);
                    } else {
                        merged.setField(f, bField);
                    }
                }
            }
        }
        return merged;
    }


}
