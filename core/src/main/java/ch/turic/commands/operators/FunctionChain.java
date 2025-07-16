package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.commands.ChainedClosure;
import ch.turic.commands.Closure;
import ch.turic.commands.ClosureOrMacro;
import ch.turic.memory.Context;

@Operator.Symbol("##")
public class FunctionChain extends AbstractOperator {

    @Override
    public Object binaryOp(Context ctx, Object op1, Command right) throws ExecutionException {
        if (op1 instanceof ClosureOrMacro) {
            final var op2 = right.execute(ctx);
            if (op2 instanceof ClosureOrMacro) {
                return new ChainedClosure((ClosureOrMacro) op1, (ClosureOrMacro) op2);
            }
        }
        final var op2 = right.execute(ctx);
        return binary("hash", op1, op2, null, null);
    }
}
