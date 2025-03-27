package javax0.turicum.memory;

import javax0.turicum.commands.Command;
import javax0.turicum.commands.ExecutionException;

import java.util.Objects;

public record ArrayElementLeftValue(LeftValue arrayLeftValue, Command index) implements LeftValue {

    public ArrayElementLeftValue {
        Objects.requireNonNull(index);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var indexValue = index.execute(ctx);
        final var guaranteedObject = arrayLeftValue.getIndexable(ctx, indexValue);
        final var existing = guaranteedObject.getIndex(indexValue);
        if (existing == null) {
            final var newObject = new LngObject(null, ctx.open());
            guaranteedObject.setIndex(indexValue, newObject);
            return newObject;
        } else {
            return LeftValue.toObject(existing);
        }
    }

    @Override
    public HasIndex getIndexable(Context ctx, Object indexValue) {
        final var leftValueIndex = index.execute(ctx);
        final var guaranteedObject = arrayLeftValue.getIndexable(ctx, leftValueIndex);
        final var existing = guaranteedObject.getIndex(leftValueIndex);
        if (existing == null) {
            // Create a new array without setting an initial index
            final HasIndex newIndexable = HasIndex.createFor(indexValue, ctx);
            guaranteedObject.setIndex(leftValueIndex, newIndexable);
            return newIndexable;
        } else {
            return LeftValue.toIndexable(existing);
        }
    }

    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        final var indexValue = index.execute(ctx);
        arrayLeftValue.getIndexable(ctx, indexValue).setIndex(indexValue, value);
    }

    @Override
    public String toString() {
        return arrayLeftValue.toString() + "[" + index.toString() + "]";
    }
}
