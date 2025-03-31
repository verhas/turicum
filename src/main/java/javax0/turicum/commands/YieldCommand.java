package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public record YieldCommand(Command expression, Command condition) implements Command {

    @Override
    public Object execute(Context context) throws ExecutionException {
        if (Cast.toBoolean(condition.execute(context))) {
            final var result = expression.execute(context);
            context.threadContext.currentYielder().send(result);
            return result;
        } else {
            return null;
        }
    }

}
