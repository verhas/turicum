package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.Objects;

public class VariableLeftValue implements LeftValue {
    public String variable() {
        return variable;
    }

    final String variable;

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

    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        ctx.update(variable, value);
    }

    @Override
    public String toString() {
        return variable;
    }
}
