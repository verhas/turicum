package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import java.util.Objects;

public record VariableLeftValue(String variable) implements LeftValue {

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
        ctx.let(variable, value);
    }

    @Override
    public String toString() {
        return variable;
    }
}
