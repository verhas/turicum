package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public record VariableLeftValue(String variable) implements LeftValue {

    public static VariableLeftValue factory(final Unmarshaller.Args args) {
        return new VariableLeftValue(args.str("variable"));
    }

    public VariableLeftValue(String variable) {
        this.variable = Objects.requireNonNull(variable);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var existing = ctx.get(variable);
        return LeftValue.toObject(existing);
    }

    @Override
    public HasIndex getIndexable(Context ctx, Object indexValue) {
        final var existing = ctx.get(variable);
        return LeftValue.toIndexable(existing, indexValue);
    }

    /**
     * Assigns a new value to the variable in the given context.
     *
     * @param ctx the context in which the variable is stored
     * @param value the value to assign to the variable
     * @throws ExecutionException if the assignment fails
     */
    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        ctx.update(variable, value);
    }
    /**
     * Updates the variable's value in the context by applying a transformation function to its current value.
     *
     * @param ctx the context containing the variable
     * @param newValueCalculator a function that computes the new value based on the current value
     * @return the new value assigned to the variable
     * @throws ExecutionException if updating the variable in the context fails
     */
    @Override
    public Object reassign(Context ctx,  Function<Object,Object> newValueCalculator) throws ExecutionException {
        final var value = ctx.get(variable);
        final var newValue = newValueCalculator.apply(value);
        ctx.update(variable, newValue);
        return newValue;
    }

    /**
     * Returns the variable name represented by this left-value.
     *
     * @return the variable name as a string
     */
    @Override
    public String toString() {
        return variable;
    }
}
