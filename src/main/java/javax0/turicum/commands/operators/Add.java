package javax0.turicum.commands.operators;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExecutionException;
import javax0.turicum.memory.Context;

@Operator.Symbol("+")
public class Add extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        if (left == null) {
            // you can write '+' in front of anything, like +"string" or even an object, that is just the same
            return right.execute(ctx);
        } else {
            final var op1 = left.execute(ctx);
            final var op2 = right.execute(ctx);

            // if the left side is a string, then convert it to a string
            if (op1 instanceof CharSequence s) {
                return s.toString() + op2;
            }

            return binary("add", op1, op2, Long::sum, Double::sum);
        }
    }

}
