package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.utils.Unmarshaller;

import java.util.Objects;
import java.util.function.Function;

public record ObjectFieldLeftValue(LeftValue object, String field) implements LeftValue {
    /**
     * Creates an ObjectFieldLeftValue instance from unmarshalling arguments.
     * <p>
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

    /**
     * Retrieves an object field within a given context, creating and assigning a new field object if it does not already exist.
     * If the field already exists, it attempts to convert it into an object representation.
     * <p>
     * If the field does not exist, it creates the field, inserts it into the object, and assigns it to a freshly created
     * class-less Turicum object.
     *
     * @param ctx the evaluation context used for resolving the field object
     * @return the object field represented by this left-value within the provided context
     */
    @Override
    public HasFields getObject(LocalContext ctx) {
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
    public HasIndex getIndexable(LocalContext ctx, Object indexValue) {
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
    public void assign(LocalContext ctx, Object value) throws ExecutionException {
        object.getObject(ctx).setField(field, value);
    }

    /**
     * Updates the value of the specified field by applying a function to its current value.
     * <p>
     * Retrieves the current value of the field from the underlying object, computes a new value using the provided function, assigns the new value to the field, and returns it.
     * <p>
     * If the object is an {@code LngObject} then it hibernates the field, so changing it is not possible during the evaluation new value calculation.
     * <p>
     * If it is not an {@code LngObject} then as the best effort it is checked afterward that the value was not changed during the new value calculation.
     * <p>
     * The first approach fails faster, at the time of the first update.
     *
     * @param ctx                the evaluation context
     * @param newValueCalculator a function that computes the new value based on the current field value
     * @return the new value assigned to the field
     * @throws ExecutionException if an error occurs during value retrieval or assignment
     */
    @Override
    public Object reassign(LocalContext ctx, Function<Object, Object> newValueCalculator) throws ExecutionException {
        final var object = this.object.getObject(ctx);
        final Object newValue;
        if (object instanceof LngObject lngObject) {
            try (final var ignore = lngObject.context().hibernate(field)) {
                final var value = lngObject.getField(field);
                newValue = newValueCalculator.apply(value);
            }
        } else {
            final var value = object.getField(field);
            newValue = newValueCalculator.apply(value);
            final var checkValue = object.getField(field);
            if (value != checkValue) {
                throw new ExecutionException("Assigned value changed while calculating new value %s.%s", this, field);
            }
        }
        object.setField(field, newValue);
        return newValue;
    }
}
