package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.Command;

import java.util.Objects;
import java.util.function.Function;

public record CalculatedLeftValue(Command expression) implements LeftValue {

    public CalculatedLeftValue(Command expression) {
        this.expression = Objects.requireNonNull(expression);
    }

    @Override
    public HasFields getObject(LocalContext ctx) {
        final var existing = expression.execute(ctx);
        return LeftValue.toObject(existing);
    }

    @Override
    public HasIndex getIndexable(LocalContext ctx, Object indexValue) {
        final var existing = expression.execute(ctx);
        return LeftValue.toIndexable(existing, indexValue);
    }

    /****
     * Throws an exception to indicate that assignment to a calculated left value is not supported.
     *
     * @throws ExecutionException always thrown to signal that assignment is disallowed
     */
    @Override
    public void assign(LocalContext ctx, Object value) throws ExecutionException {
        throw new ExecutionException("Cannot assign value to calculated left side");
    }

    /****
     * Throws an exception to indicate that reassignment of a calculated left value is not supported.
     *
     * @throws ExecutionException always thrown to signal that modification is not allowed
     */
    @Override
    public Object reassign(LocalContext ctx, Function<Object, Object> newValueCalculator) throws ExecutionException {
        throw new ExecutionException("Cannot modify left value to calculated value");
    }

    /**
     * Returns a string representation of this calculated left value, displaying the encapsulated expression in curly braces.
     *
     * @return a string in the format "{expression}"
     */
    @Override
    public String toString() {
        return "{" + expression + "}";
    }
}
