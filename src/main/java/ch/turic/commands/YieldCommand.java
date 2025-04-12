package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;

public class YieldCommand extends AbstractCommand {
    final Command expression;
    final Command condition;

    public Command condition() {
        return condition;
    }

    public Command expression() {
        return expression;
    }

    public YieldCommand(Command expression, Command condition) {
        this.expression = expression;
        this.condition = condition;
    }

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        if( Cast.toBoolean(condition.execute(context))) {
            final var result = expression.execute(context);
            context.threadContext.currentYielder().send(result);
            return result;
        }else{
            return null;
        }
    }
}
