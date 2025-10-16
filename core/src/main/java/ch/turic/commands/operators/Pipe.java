package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;

@Operator.Symbol("or")
public class Pipe extends AbstractOperator {

    /**
     * Performs a binary operation between the given operands. If the first operand (op1)
     * is not null, it is returned. Otherwise, the command represented by the right operand
     * is executed and its result is returned.
     *
     * @param ctx   The execution context for the binary operation
     * @param op1   The left operand of the binary operation
     * @param right The right operand of the binary operation, represented as a command
     *              to be executed if the left operand is null
     * @return The result of the binary operation, which is either the left operand if it
     * is not null, or the result of executing the right operand command
     * @throws ExecutionException If there is an error during the execution of the right operand
     */
    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        if (op1 != null) {
            return op1;
        }
        return right.execute(ctx);
    }
}
