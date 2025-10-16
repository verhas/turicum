package ch.turic.commands.operators;

import ch.turic.Command;
import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.BlockCommand;
import ch.turic.commands.EmptyObject;
import ch.turic.memory.LocalContext;

@Operator.Symbol("'")
public class Quote implements Operator {

    @Override
    public Object execute(LocalContext ctx, Command left, Command right) throws ExecutionException {
        if (left != null) {
            throw new ExecutionException("Somehow ' (quote) is used as a binary operator");
        }
        if (right instanceof EmptyObject) {
            return BlockCommand.EMPTY_BLOCK;
        }
        return right;
    }
}
