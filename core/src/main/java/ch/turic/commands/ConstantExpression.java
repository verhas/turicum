package ch.turic.commands;

import ch.turic.ExecutionException;
import ch.turic.memory.Context;
import ch.turic.utils.Unmarshaller;

public class ConstantExpression extends AbstractCommand {
    public Object value() {
        return value;
    }


    public static ConstantExpression factory(final Unmarshaller.Args args) {
        final var value = args.get("value", Object.class);
        return new ConstantExpression(value);
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
