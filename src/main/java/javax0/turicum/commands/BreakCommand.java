package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public record BreakCommand(Command expression, Command condition) implements Command {

    @Override
    public Conditional execute(Context context) throws ExecutionException {
        if( Cast.toBoolean(condition.execute(context))) {
            return Conditional.doBreak(expression.execute(context));
        }else{
            return Conditional.result(null);
        }
    }
}
