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

    /****
     * Creates an `IncrementDecrement` instance from the provided arguments.
     *
     * @param args the arguments containing the left value, increment flag, and post flag
     * @return a new `IncrementDecrement` command configured according to the arguments
     */
    public static IncrementDecrement factory(Unmarshaller.Args args) {
        return new IncrementDecrement(args.get("leftValue", LeftValue.class),
                args.bool("increment"),
                args.bool("post"));
    }

    /**
     * Constructs an IncrementDecrement command for incrementing or decrementing a LeftValue.
     *
     * @param leftValue the target LeftValue to be modified
     * @param increment true to perform increment, false for decrement
     * @param post true for post-fix operation, false for pre-fix
     */
    public IncrementDecrement(LeftValue leftValue, boolean increment, boolean post) {
        this.leftValue = leftValue;
        this.increment = increment;
        this.post = post;

    }

    /**
     * Executes the increment or decrement operation on the target LeftValue within the given context.
     * <p>
     * Depending on the configuration, performs either a pre- or post-increment/decrement and returns the appropriate value.
     *
     * @param ctx the execution context
     * @return the value before the operation if post-fix, or after the operation if pre-fix
     * @throws ExecutionException if the increment or decrement operation cannot be performed
     */
    @Override
    public Object _execute(final Context ctx) throws ExecutionException {
        ctx.step();
        AtomicReference<Object> result = new AtomicReference<>();
        final Function<Object, Object> converter = increment ? (x) -> applyDelta(ctx, x, result, 1) : (x) -> applyDelta(ctx, x, result, -1);
        final var newValue = leftValue.reassign(ctx, converter);
        final var oldValue = result.get();
        return post ? oldValue : newValue;
    }

    /**
     * Increments/decrements the given value by one, supporting both numeric primitives and custom object types.
     * <p>
     * If the value is a {@code Long} or {@code Double}, returns the value incremented by one.
     * For other types, attempts to perform the increment operation by invoking the "++" operator method
     * on the object if available.
     *
     * @param value the value to increment
     * @param result an atomic reference in which the original value is stored
     * @return the incremented value
     * @throws ExecutionException if the increment operation cannot be performed on the value
     */
    private Object applyDelta(Context ctx, Object value, AtomicReference<Object> result, int delta) throws ExecutionException {
        result.set(value);
        if (Cast.isLong(value)) {
            return Cast.toLong(value) + delta;
        }
        if (Cast.isDouble(value)) {
            return Cast.toDouble(value) + delta;
        }
        final var operator = delta > 0 ? "++" : "--";
        if (value instanceof LngObject lngObject) {
            final var method = lngObject.getField(operator);
            if (method != null) {
                if (!(method instanceof Closure operation)) {
                    throw new ExecutionException(String.format("%s is not a valid %s operator", method, operator));
                }
                return operation.callAsMethod(ctx, lngObject, operator);
            }
        }
        throw new ExecutionException("Cannot apply operator %s on value '%s'", operator, value);
    }
}
