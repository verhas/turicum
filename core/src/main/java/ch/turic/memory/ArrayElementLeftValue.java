package ch.turic.memory;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public record ArrayElementLeftValue(LeftValue arrayLeftValue, Command index) implements LeftValue {

    /****
     * Creates an ArrayElementLeftValue from unmarshalling arguments.
     *
     * Extracts the array left value and index command from the provided arguments to construct a new ArrayElementLeftValue instance.
     *
     * @param args the unmarshalling arguments containing the array left value and index command
     * @return a new ArrayElementLeftValue instance
     */
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

    /**
     * Assigns the specified value to the element at the computed index of the underlying array left value.
     *
     * After assignment, updates the underlying left value if the indexed element is a mutable string representation,
     * ensuring that changes to mutable string objects are reflected in the original left value.
     *
     * @param ctx the execution context
     * @param value the value to assign at the computed index
     * @throws ExecutionException if index evaluation or assignment fails
     */
    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        final var indexValue = index.execute(ctx);
        final var indexable = arrayLeftValue.getIndexable(ctx, indexValue);
        indexable.setIndex(indexValue, value);
        // IndexedString is a copy of the string, after the change we have to replace the left value
        updateString(ctx, indexable);
    }

    /**
     * Updates the element at the specified index by applying a function to its current value.
     *
     * Executes the index command to determine the target index, retrieves the current value at that index,
     * applies the provided function to compute a new value, and sets this new value at the index. If the
     * underlying indexable is an `IndexedString` wrapping a `StringBuilder`, the updated string is reassigned
     * to the original array left value to ensure string modifications are propagated.
     *
     * @param ctx the execution context
     * @param newValueCalculator a function that computes the new value based on the current value at the index
     * @return the new value assigned at the specified index
     * @throws ExecutionException if index evaluation or assignment fails
     */
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
     * Ensures that if the given indexable object is an {@code IndexedString} backed by a {@code StringBuilder}, the updated string is assigned back to the underlying array left value.
     *
     * This method maintains correct string semantics by propagating changes made to mutable string representations (via {@code StringBuilder}) back to the original variable, since Java strings are immutable.
     *
     * @param ctx the execution context
     * @param indexable the indexable object to check and update if necessary
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
