package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.commands.Command;

import java.util.Objects;

public record CalculatedLeftValue(Command expression) implements LeftValue {

    public CalculatedLeftValue(Command expression) {
        this.expression = Objects.requireNonNull(expression);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var existing = expression.execute(ctx);
        return LeftValue.toObject(existing);
    }

    @Override
    public HasIndex getIndexable(Context ctx, Object indexValue) {
        final var existing = expression.execute(ctx);
        return LeftValue.toIndexable(existing, indexValue);
    }

    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        throw new ExecutionException("Cannot assign left value to calculated value");
    }

    @Override
    public String toString() {
        return "{" + expression + "}";
    }
}
