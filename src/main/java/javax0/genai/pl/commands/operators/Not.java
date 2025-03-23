package javax0.genai.pl.commands.operators;

import javax0.genai.pl.commands.Command;
import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.memory.Context;

@Operator.Symbol("!")
public class Not extends AbstractNumericOperator {

    @Override
    public Object execute(Context ctx, Command left, Command right) throws ExecutionException {
        ExecutionException.when(left != null,"Somehow '!' is used as a binary operator");
        final var op2 = right.execute(ctx);
        ExecutionException.when(!Cast.isBoolean(op2), "%s cannot be used as boolean", op2);
        return !Cast.toBoolean(op2);
    }
}
