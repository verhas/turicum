package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public record ReturnCommand(Command expression, Command condition) implements Command {

    @Override
    public Conditional execute(Context context) throws ExecutionException {
        if( Cast.toBoolean(condition.execute(context))) {
            return Conditional.doReturn(expression.execute(context));
        }else{
            return Conditional.result(null);
        }
    }
}
