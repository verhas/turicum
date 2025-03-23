package javax0.genai.pl.commands.operators;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.memory.Context;

@Operator.Symbol("%")
public class Mod extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        final var op1 = left.execute(ctx);
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            throw new ExecutionException("Cannot '%s' %% '%s'", op2, op1);
        }
        return binary("mod", op1, op2, (a, b) -> a % b, (a, b) -> a % b);
    }
}
