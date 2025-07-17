package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.memory.LngList;

import java.util.HashSet;

@Operator.Symbol("&")
public class Band extends AbstractOperator {

    /**
     * Performs a bitwise AND operation or list intersection based on operand types.
     * <p>
     * If both operands are instances of {@code LngList}, returns a new {@code LngList} containing elements present in both lists (intersection).
     * If only the left operand is a {@code LngList}, returns a new {@code LngList} containing the right operand if it exists in the left list.
     * Otherwise, performs a bitwise AND operation on the two operands.
     *
     * @param ctx the execution context
     * @param op1 the left operand, which may be a {@code LngList} or a numeric value
     * @param right the command representing the right operand
     * @return the result of the bitwise AND or list intersection
     * @throws ExecutionException if an error occurs during command execution
     */
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

    /****
     * Returns a new LngList containing unique elements present in both input lists.
     * <p>
     * The resulting list preserves the order of elements as they appear in the first list and excludes duplicates.
     *
     * @param list1 the first list to compare
     * @param list2 the second list to compare
     * @return a LngList of elements found in both list1 and list2
     */
    private static LngList mergeLists(LngList list1, LngList list2) {
        final var result = new LngList(list1.getFieldProvider());
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

