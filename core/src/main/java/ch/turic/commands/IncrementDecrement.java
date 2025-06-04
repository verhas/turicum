package ch.turic.commands;


import ch.turic.ExecutionException;
import ch.turic.commands.operators.Cast;
import ch.turic.memory.Context;
import ch.turic.memory.LeftValue;
import ch.turic.memory.LngObject;
import ch.turic.utils.Unmarshaller;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class IncrementDecrement extends AbstractCommand {
    final LeftValue leftValue;
    final boolean increment;
    final boolean post;

    public static IncrementDecrement factory(Unmarshaller.Args args) {
        return new IncrementDecrement(args.get("leftValue", LeftValue.class),
                args.bool("increment"),
                args.bool("post"));
    }

    public IncrementDecrement(LeftValue leftValue, boolean increment, boolean post) {
        this.leftValue = leftValue;
        this.increment = increment;
        this.post = post;

    }

    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        AtomicReference<Object> result = new AtomicReference<>();
        final Function<Object, Object> converter = increment ? (x) -> increment(ctx, x, result) : (x) -> decrement(ctx, x, result);
        final var newValue = leftValue.reassign(ctx, converter);
        final var oldValue = result.get();
        return post ? oldValue : newValue;
    }

    private Object increment(Context ctx, Object value, AtomicReference<Object> result) throws ExecutionException {
        result.set(value);
        if (Cast.isLong(value)) {
            return Cast.toLong(value) + 1L;
        }
        if (Cast.isDouble(value)) {
            return Cast.toDouble(value) + 1D;
        }
        return operateOnObject(ctx, value, "++");
    }

    private Object decrement(Context ctx, Object value, AtomicReference<Object> result) {
        result.set(value);
        if (Cast.isLong(value)) {
            return Cast.toLong(value) - 1L;
        }
        if (Cast.isDouble(value)) {
            return Cast.toDouble(value) - 1D;
        }
        return operateOnObject(ctx, value, "--");
    }

    private Object operateOnObject(Context ctx, Object value, String operator) {
        if (value instanceof LngObject lngObject) {
            final var method = lngObject.getField(operator);
            if (method != null) {
                if (!(method instanceof Closure operation)) {
                    throw new ExecutionException(String.format("%s is not a valid %s operator", method, operator));
                }
                return operation.callAsMethod(ctx, lngObject, operator);
            }
        }
        throw new ExecutionException("Cannot increment value '%s'", value);
    }

}
