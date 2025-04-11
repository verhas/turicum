package javax0.turicum.commands;

import javax0.turicum.ExecutionException;
import javax0.turicum.memory.Context;

public class ConstantExpression extends AbstractCommand {
    public Object value() {
        return value;
    }

    public ConstantExpression(Object value) {
        this.value = value;
    }

    final Object value;

    @Override
    public Object _execute(final Context context) throws ExecutionException {
        return value;
    }
}
