package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.Sentinel;

public class BreakCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command expression() {
        return expression;
    }

    public Command condition() {
        return condition;
    }

    public BreakCommand(Command expression, Command condition) {
        this.expression = expression;
        this.condition = condition;
    }

    @Override
    public Conditional _execute(Context context) throws ExecutionException {
        if (Cast.toBoolean(condition.execute(context))) {
            if (expression == null) {
                return Conditional.doBreak(Sentinel.NO_VALUE);
            } else {
                return Conditional.doBreak(expression.execute(context));
            }
        } else {
            return null;
        }
    }
}
