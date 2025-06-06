package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public record ObjectFieldLeftValue(LeftValue object, String field) implements LeftValue {
    /**
     * Creates an ObjectFieldLeftValue instance from unmarshalling arguments.
     *
     * Extracts a LeftValue representing the target object and a field name string from the provided arguments.
     *
     * @param args the unmarshalling arguments containing the object and field information
     * @return a new ObjectFieldLeftValue for the specified object and field
     */
    public static ObjectFieldLeftValue factory(final Unmarshaller.Args args) {
        return new ObjectFieldLeftValue(
                args.get("object", LeftValue.class),
                args.str("field")
        );
    }

    public ObjectFieldLeftValue {
        Objects.requireNonNull(field);
    }

    @Override
    public HasFields getObject(Context ctx) {
        final var guaranteedObject = object.getObject(ctx);
        final var existing = guaranteedObject.getField(field);
        if (existing == null) {
            final var newObject = LngObject.newEmpty(ctx);
            guaranteedObject.setField(field, newObject);
            return newObject;
        } else {
            return LeftValue.toObject(existing);
        }
    }

    @Override
    public HasIndex getIndexable(Context ctx, Object indexValue) {
        final var guaranteedObject = object.getObject(ctx);
        final var existing = guaranteedObject.getField(field);
        if (existing == null) {
            final HasIndex newIndexable = HasIndex.createFor(indexValue, ctx);
            guaranteedObject.setField(field, newIndexable);
            return newIndexable;
        } else {
            return LeftValue.toIndexable(existing);
        }
    }

    /****
     * Assigns the specified value to the target field of the object represented by this left-value within the given context.
     *
     * @param ctx the evaluation context
     * @param value the value to assign to the field
     * @throws ExecutionException if the assignment operation fails
     */
    @Override
    public void assign(Context ctx, Object value) throws ExecutionException {
        object.getObject(ctx).setField(field, value);
    }

    /****
     * Updates the value of the specified field by applying a function to its current value.
     *
     * Retrieves the current value of the field from the underlying object, computes a new value using the provided function, assigns the new value to the field, and returns it.
     *
     * @param ctx the evaluation context
     * @param newValueCalculator a function that computes the new value based on the current field value
     * @return the new value assigned to the field
     * @throws ExecutionException if an error occurs during value retrieval or assignment
     */
    @Override
    public Object reassign(Context ctx, Function<Object, Object> newValueCalculator) throws ExecutionException {
        final var value = object.getObject(ctx).getField(field);
        final var newValue = newValueCalculator.apply(value);
        object.getObject(ctx).setField(field, newValue);
        return newValue;
    }
}
