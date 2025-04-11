package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

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
    public Conditional execute(Context context) throws ExecutionException {
        if (Cast.toBoolean(condition.execute(context))) {
            return Conditional.doBreak(expression.execute(context));
        } else {
            return Conditional.result(null);
        }
    }
}
