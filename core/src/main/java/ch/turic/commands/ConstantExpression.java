package ch.turic.commands;

import ch.turic.exceptions.ExecutionException;
import ch.turic.memory.LocalContext;
import ch.turic.utils.Unmarshaller;

public class ConstantExpression extends AbstractCommand {
    public Object value() {
        return value;
    }


    public static ConstantExpression factory(final Unmarshaller.Args args) {
        return new ConstantExpression(args.get("value", Object.class)).fixPosition(args);
    }

    public ConstantExpression(Object value) {
        this.value = value;
    }

    final Object value;

    @Override
    public Object _execute(final LocalContext context) throws ExecutionException {
        return value;
    }
}
