package javax0.genai.pl.commands.operators;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.memory.Context;

import java.util.regex.Pattern;

@Operator.Symbol("-")
public class Subtract extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        if (left == null) {
            final var op = right.execute(ctx);
            return unary("negate", op, b -> -b, b -> -b);
        } else {
            final var op1 = left.execute(ctx);
            final var op2 = right.execute(ctx);

            // if the left side is a string, then convert it to a string
            if (op1 instanceof CharSequence s) {
                // remove all occurrences of op2 from op1
                return s.toString().replaceAll(Pattern.quote(op2.toString()), "");
            }
            return binary("subtract", op1, op2, (a, b) -> a - b, (a, b) -> a - b);
        }
    }

}
