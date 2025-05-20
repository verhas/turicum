package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;

public class DieCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command expression() {
        return expression;
    }

    public Command condition() {
        return condition;
    }

    public DieCommand(Command expression, Command condition) {
        this.expression = expression;
        this.condition = condition;
    }

    @Override
    public Conditional _execute(Context context) throws ExecutionException {
        if (Cast.toBoolean(condition.execute(context))) {
            if (expression == null) {
                throw new ExecutionException("");
            } else {
                final var exception = expression.execute(context);
                if (exception != null) {
                    throw new ExecutionException(exception.toString());
                } else {
                    throw new ExecutionException("none");
                }
            }
        } else {
            return null;
        }
    }
}
