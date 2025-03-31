package javax0.turicum.commands.operators;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Command;
import javax0.turicum.memory.Context;
import javax0.turicum.memory.InfiniteValue;

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
        return binary("subtract", op1, op2, (a, b) -> a - b, (a, b) -> a - b);
    }

}
