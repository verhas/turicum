package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public class ReturnCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command condition() {
        return condition;
    }

    public Command expression() {
        return expression;
    }

    public ReturnCommand(Command expression, Command condition) {
        this.condition = condition;
        this.expression = expression;
    }

    @Override
    public Conditional _execute(Context context) throws ExecutionException {
        if (Cast.toBoolean(condition.execute(context))) {
            return Conditional.doReturn(expression.execute(context));
        } else {
            return Conditional.result(null);
        }
    }
}
