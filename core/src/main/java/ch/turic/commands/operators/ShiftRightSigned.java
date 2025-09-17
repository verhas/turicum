package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.Command;
import ch.turic.commands.Conditional;
import ch.turic.memory.LocalContext;

@Operator.Symbol(">>>")
public class ShiftRightSigned extends AbstractOperator {

    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            ExecutionException.when(op2 instanceof Conditional, "Cannot append break or return value to a string.");
            return s.toString() + op2;
        }

        return binary("sshr", op1, op2, (a, b) -> (a >>> b), null);
    }

}
