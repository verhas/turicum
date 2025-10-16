package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.ChainedClosure;
import ch.turic.commands.ClosureLike;
import ch.turic.memory.LocalContext;

@Operator.Symbol("##")
public class FunctionChain extends AbstractOperator {

    @Override
    public Object binaryOp(LocalContext ctx, Object op1, Command right) throws ExecutionException {
        if (op1 instanceof ClosureLike) {
            final var op2 = right.execute(ctx);
            if (op2 instanceof ClosureLike) {
                return new ChainedClosure((ClosureLike) op1, (ClosureLike) op2);
            }
        }
        final var op2 = right.execute(ctx);
        return binary("hash", op1, op2, null, null);
    }
}
