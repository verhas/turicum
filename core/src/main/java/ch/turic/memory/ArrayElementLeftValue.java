package ch.turic.memory;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public record ArrayElementLeftValue(LeftValue arrayLeftValue, Command index) implements LeftValue {

    public static ArrayElementLeftValue factory(final Unmarshaller.Args args) {
        return new ArrayElementLeftValue(
                args.get("arrayLeftValue", LeftValue.class),
                args.command("index")
        );
    }

    public ArrayElementLeftValue {
        Objects.requireNonNull(index);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var indexValue = index.execute(ctx);
        final var guaranteedObject = arrayLeftValue.getIndexable(ctx, indexValue);
        final var existing = guaranteedObject.getIndex(indexValue);
        if (existing == null) {
            final var newObject = LngObject.newEmpty(ctx);
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
        final var indexable = arrayLeftValue.getIndexable(ctx, indexValue);
        indexable.setIndex(indexValue, value);
        // IndexedString is a copy of the string, after the change we have to replace the left value
        updateString(ctx, indexable);
    }

    @Override
    public Object reassign(Context ctx, Function<Object, Object> newValueCalculator) throws ExecutionException {
        final var indexValue = index.execute(ctx);
        final var indexable = arrayLeftValue.getIndexable(ctx, indexValue);
        final var value = indexable.getIndex(indexValue);
        final var newValue = newValueCalculator.apply(value);
        indexable.setIndex(indexValue, newValue);
        // IndexedString is a copy of the string, after the change we have to replace the left value
        updateString(ctx, indexable);
        return newValue;
    }

    /**
     * Updates the string value of the given indexable object if it is an instance of {@code IndexedString}.
     * Specifically, it replaces the associated left value with the updated string contained in the indexable object.
     * <p>
     * This method is necessary because strings in Java are immutable - once created, they cannot be modified.
     * In Turicum, to support mutable string operations (like array indexing and modifications), strings are
     * internally represented using StringBuilder. When a string is modified through array indexing, a new
     * string value must be created and assigned back to the original variable, which is handled by this method.
     *
     * @param ctx       the execution context used for the operation
     * @param indexable the object to be checked and potentially updated; must implement {@code HasIndex}
     */
    private void updateString(Context ctx, HasIndex indexable) {
        if (indexable instanceof IndexedString(StringBuilder string)) {
            arrayLeftValue.assign(ctx, string.toString());
        }
    }

    @Override
    public String toString() {
        return arrayLeftValue.toString() + "[" + index.toString() + "]";
    }
}
