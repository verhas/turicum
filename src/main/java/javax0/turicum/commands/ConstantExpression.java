package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

public record ConstantExpression(Object value) implements Command {
    @Override
    public Object execute(Context context) throws ExecutionException {
        return value;
    }
}
