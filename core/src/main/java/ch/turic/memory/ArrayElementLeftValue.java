package ch.turic.memory;

import ch.turic.Command;
import ch.turic.ExecutionException;
import ch.turic.utils.Unmarshaller;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;

public record ArrayElementLeftValue(LeftValue arrayLeftValue, Command index) implements LeftValue {

    /****
     * Creates an ArrayElementLeftValue from unmarshalling arguments.
     * <p>
     * Extracts the array left value and index command from the provided arguments to construct a new ArrayElementLeftValue instance.
     *
     * @param args the unmarshalling arguments containing the array left value and index command
     * @return a new ArrayElementLeftValue instance
     */
    @SuppressWarnings("unused")
    public static ArrayElementLeftValue factory(final Unmarshaller.Args args) {
        return new ArrayElementLeftValue(
                args.get("arrayLeftValue", LeftValue.class),
                args.command("index")
        );
    }

    public ArrayElementLeftValue {
        Objects.requireNonNull(index);
    }

    /**
     * Retrieves or creates an object within a specified context based on a computed index.
     * If no object exists at the computed index, a new empty object is instantiated and assigned
     * to that index. Otherwise, the existing object is transformed and returned.
     * <p>
     * Note that the underlying array may also be extended with null elements if it did not have the index.
     *
     * @param ctx the execution context used for evaluating the index and managing object state
     * @return the object located at the computed index or a newly instantiated object if none exists
     */
    @Override
    public HasFields getObject(LocalContext ctx) {
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
    public HasIndex getIndexable(LocalContext ctx, Object indexValue) {
        final var leftValueIndex = index.execute(ctx);
        final var guaranteedObject = arrayLeftValue.getIndexable(ctx, leftValueIndex);
        final var existing = guaranteedObject.getIndex(leftValueIndex);
        if (existing == null) {
            // Create a new array without setting an initial index
            final HasIndex newIndexable = HasIndex.createFor(indexValue, ctx);
            guaranteedObject.setIndex(leftValueIndex, newIndexable);
            return new WithIndexedContainer(newIndexable, guaranteedObject, leftValueIndex);
        } else {
            return new WithIndexedContainer(LeftValue.toIndexable(existing), guaranteedObject, leftValueIndex);
        }
    }

    /**
     * This class implements {@link HasIndex} delegating functionality to the field {@code indexed}.
     * <p>
     * At the same time, it also holds the container and the index that we used to get access to this object.
     * The container is also a {@link HasIndex} object, and the index is the index object.
     * <p>
     * This object is used when a string is updated. In that case the string has to be replaced, and if the string
     * was stored in an indexed left value, then we have to remember the index.
     * Without this information we would need to execute the expression giving the index to update the indexed left value.
     * (It was the case until and including 1.1.0).
     * It is problematic when the expression has side effects.
     * <p>
     * For example
     *
     * <pre>
     *     {@code
     * mut sa = [ ["alma", "korte"],["alma", "korte"] ];
     * mut i = 0;
     * mut j = 0;
     * sa[ i++ ][ j++ ][3] = "Z";
     * println sa;
     *     }
     * </pre>
     * <p>
     * Updated `sa` in a wrong way.
     */
    public record WithIndexedContainer(HasIndex indexed, HasIndex container, Object lastIndex) implements HasIndex {
        public void set(Object value) {
            container.setIndex(lastIndex, value);
        }

        @Override
        public void setIndex(Object index, Object value) throws ExecutionException {
            indexed.setIndex(index, value);
        }

        @Override
        public Object getIndex(Object index) throws ExecutionException {
            return indexed.getIndex(index);
        }

        @Override
        public Iterator<Object> iterator() {
            return indexed.iterator();
        }
    }

    /**
     * Assigns the specified value to the element at the computed index of the underlying array left value.
     * <p>
     * After assignment, updates the underlying left value if the indexed element is a mutable string representation,
     * ensuring that changes to mutable string objects are reflected in the original left value.
     *
     * @param ctx   the execution context
     * @param value the value to assign at the computed index
     * @throws ExecutionException if index evaluation or assignment fails
     */
    @Override
    public void assign(LocalContext ctx, Object value) throws ExecutionException {
        final var indexValue = index.execute(ctx);
        final var indexable = arrayLeftValue.getIndexable(ctx, indexValue);
        indexable.setIndex(indexValue, value);
        // IndexedString is a copy of the string, after the change we have to replace the left value
        updateString(ctx, indexable);
    }

    /**
     * Updates the element at the specified index by applying a function to its current value.
     * <p>
     * Executes the index command to determine the target index, retrieves the current value at that index,
     * applies the provided function to compute a new value, and sets this new value at the index. If the
     * underlying indexable is an `IndexedString` wrapping a `StringBuilder`, the updated string is reassigned
     * to the original array left value to ensure string modifications are propagated.
     *
     * @param ctx                the execution context
     * @param newValueCalculator a function that computes the new value based on the current value at the index
     * @return the new value assigned at the specified index
     * @throws ExecutionException if index evaluation or assignment fails
     */
    @Override
    public Object reassign(LocalContext ctx, Function<Object, Object> newValueCalculator) throws ExecutionException {
        final var indexValue = index.execute(ctx);
        final var indexable = arrayLeftValue.getIndexable(ctx, indexValue);
        final Object value;
        final Object newValue;
        try (LocalContext.VariableHibernation vh = getVariableHibernation(ctx)) {
            value = indexable.getIndex(indexValue);
            newValue = newValueCalculator.apply(value);
        }
        final var checkValue = indexable.getIndex(indexValue);
        assertNoChange(value, checkValue);
        indexable.setIndex(indexValue, newValue);
        // IndexedString is a copy of the string, after the change we have to replace the left value
        updateString(ctx, indexable);
        return newValue;
    }

    /**
     * Retrieves variable hibernation information from the provided execution context based on
     * the type of the left value associated with this object.
     * <p>
     * If the left value is an array element and cannot be hibernated, then return a fake hibernation that can be
     * safely closed with no effect at the end.
     *
     * @param ctx the execution context used for processing the left value and determining the variable hibernation state
     * @return a VariableHibernation instance representing the state of the variable in the given context
     * @throws ExecutionException if the left value is a calculated value or any other issue related to assignment occurs
     */
    private LocalContext.VariableHibernation getVariableHibernation(LocalContext ctx) {
        return switch (arrayLeftValue) {
            case CalculatedLeftValue ignored ->
                    throw new ExecutionException("Cannot change the part of a calculated string. Left side of the assignment has to be left value.");
            case VariableLeftValue leftValue -> ctx.hibernate(leftValue.variable());
            case ObjectFieldLeftValue leftValue -> {
                final var obj = leftValue.getObject(ctx);
                if (obj instanceof LngObject lngObject) {
                    yield lngObject.context().hibernate(leftValue.field());
                } else {
                    yield ctx.new VariableHibernation();
                }
            }
            default -> ctx.new VariableHibernation();//fake, can be closed, does nothing
        };
    }

    /**
     * Asserts that the given value has not changed.
     * Throws an {@code ExecutionException} if the values are not equal.
     * <p>
     *
     * @param value      the value to be checked for changes
     * @param checkValue the reference value to compare against
     * @throws ExecutionException if the values are not equal
     */
    private void assertNoChange(Object value, Object checkValue) {
        if (!Objects.equals(value, checkValue)) {
            throw new ExecutionException("Assigned value changed while calculating new value %s", this);
        }
    }

    /**
     * Ensures that if the given indexable object is an {@code IndexedString} backed by a {@code StringBuilder}, the updated string is assigned back to the underlying array left value.
     * <p>
     * This method maintains correct string semantics by propagating changes made to mutable string representations (via {@code StringBuilder}) back to the original variable, since Java strings are immutable.
     *
     * @param ctx       the execution context
     * @param indexable the indexable object to check and update if necessary
     */
    private void updateString(LocalContext ctx, HasIndex indexable) {
        HasIndex embedded = indexable;
        while (embedded instanceof WithIndexedContainer wic) {
            embedded = wic.indexed;
        }
        if (embedded instanceof IndexedString(StringBuilder string)) {
            if (indexable instanceof WithIndexedContainer withIndexedContainer) {
                withIndexedContainer.set(string.toString());
            } else {
                arrayLeftValue.assign(ctx, string.toString());
            }
        }
    }

    @Override
    public String toString() {
        return arrayLeftValue.toString() + "[" + index.toString() + "]";
    }
}
