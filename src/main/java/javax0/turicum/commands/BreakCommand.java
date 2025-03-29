package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.operators.Cast;
import javax0.turicum.memory.Context;

public record BreakCommand(Command expression, Command condition) implements Command {
    public record BreakResult(Object result, boolean doBreak){}

    @Override
    public BreakResult execute(Context context) throws ExecutionException {
        if( Cast.toBoolean(condition.execute(context))) {
            return new BreakResult(expression.execute(context),true);
        }else{
            return new BreakResult(null,false);
        }
    }
}
