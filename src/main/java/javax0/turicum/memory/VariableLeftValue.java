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
        if (existing == null) {
            final var newObject = new LngObject(null, ctx.open());
            ctx.let(variable, newObject);
            return newObject;
        } else {
            return LeftValue.toObject(existing);
        }
    }

    @Override
    public HasIndex getIndexable(Context ctx, Object indexValue) {
        final var existing = ctx.get(variable);
        if (existing == null) {
            final var newIndexable = HasIndex.createFor(indexValue, ctx);
            ctx.let(variable, newIndexable);
            return newIndexable;
        } else {
            return LeftValue.toIndexable(existing);
        }
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
