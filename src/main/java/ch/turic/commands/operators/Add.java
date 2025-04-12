package ch.turic.commands.operators;

import ch.turic.ExecutionException;
import ch.turic.commands.Command;
import ch.turic.commands.Conditional;
import ch.turic.memory.Context;

@Operator.Symbol("+")
public class Add extends AbstractOperator {

    /**
     * you can write '+' in front of anything, like +"string" or even an object, that is just the same
     */
    @Override
    public Object unaryOp(Context ctx, Object op) throws ExecutionException {
        return op;
    }

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        final var op2 = right.execute(ctx);

        // if the left side is a string, then convert it to a string
        if (op1 instanceof CharSequence s) {
            ExecutionException.when(op2 instanceof Conditional, "Cannot append break or return value to a string.");
            return s.toString() + op2;
        }

        return binary("add", op1, op2, Long::sum, Double::sum);
    }

}
