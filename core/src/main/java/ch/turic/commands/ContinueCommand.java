package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;

public class ContinueCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command expression() {
        return expression;
    }

    public Command condition() {
        return condition;
    }

    public ContinueCommand(Command expression, Command condition) {
        this.expression = expression;
        this.condition = condition;
    }

    @Override
    public Conditional _execute(Context context) throws ExecutionException {
        throw new ExecutionException("Continue executed in non-loop.");
    }
}
