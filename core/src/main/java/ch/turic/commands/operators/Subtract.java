package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.commands.Command;
import ch.turic.memory.Context;
import ch.turic.memory.InfiniteValue;
import ch.turic.memory.LngList;

import java.util.regex.Pattern;

@Operator.Symbol("-")
public class Subtract extends AbstractOperator {

    @Override
    public Object unaryOp(Context ctx, Object op) throws ExecutionException {
        if (Cast.isLong(op)) {
            return -Cast.toLong(op);
        }
        if (Cast.isDouble(op)) {
            return -Cast.toDouble(op);
        }

        if( op instanceof InfiniteValue iv){
            return iv.negate();
        }

        return Reflect.getUnaryMethod("negate", op).map(Reflect.Op::callMethod)
                .orElseThrow(() -> new ExecutionException("Cannot %s.negate()", op));
    }

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            // remove all occurrences of op2 string from op1 string
            return s.toString().replaceAll(Pattern.quote(op2.toString()), "");
        }

        if( op1 instanceof LngList list1){
            final var diff = new LngList();
            diff.array.addAll(list1.array);
            if( op2 instanceof LngList list2){
                diff.array.removeAll(list2.array);
            }else{
                diff.array.remove(op2);
            }
            return diff;
        }

        return binary("subtract", op1, op2, (a, b) -> a - b, (a, b) -> a - b);
    }

}
