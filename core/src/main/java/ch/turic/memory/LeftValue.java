package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface LeftValue {

    HasFields getObject(Context ctx) throws ExecutionException;

    /**
 * Retrieves an indexable object from the given context using the specified index value.
 *
 * @param ctx the execution context
 * @param indexValue the value used to determine the indexable object
 * @return an object that supports indexed access
 * @throws ExecutionException if the indexable object cannot be resolved
 */
HasIndex getIndexable(Context ctx, Object indexValue) throws ExecutionException;

    /****
 * Assigns the specified value to this left value within the given execution context.
 *
 * @param ctx the execution context in which the assignment occurs
 * @param value the value to assign
 * @throws ExecutionException if the assignment fails or is not permitted
 */
void assign(Context ctx, Object value) throws ExecutionException;
    /****
 * Updates the value represented by this left value by applying a transformation function to its current value.
 *
 * @param newValueCalculator a function that computes the new value based on the current value
 * @return the updated value after applying the transformation
 * @throws ExecutionException if the reassignment fails or an error occurs during value computation
 */
Object reassign(Context ctx, Function<Object,Object> newValueCalculator) throws ExecutionException;

    /**
     * Converts an object to a {@link HasFields} instance.
     *
     * <p>If the object already implements {@code HasFields}, it is returned as is. If it is a {@code Map}, a new {@code MapObject} is created with an immutable copy of the map. Otherwise, the object is wrapped in a {@code JavaObject}.</p>
     *
     * @param existing the object to convert
     * @return a {@code HasFields} representation of the input object
     */
    static HasFields toObject(Object existing) {
        if (existing instanceof HasFields hasFields) {
            return hasFields;
        }
        if (existing instanceof Map<?, ?> map) {
            return new MapObject(Map.copyOf(map));
        }
        return new JavaObject(existing);
    }

    static HasIndex toIndexable(final Object existing, Object indexValue) {
        if (indexValue instanceof CharSequence) {
            ExecutionException.when(existing == null, "Cannot used None as object.");
            return switch (existing) {
                case LngObject object -> object;
                case LngClass klass -> klass;
                case HasFields fieldHaber -> fieldHaber;
                case Map<?,?> map -> new MapObject((Map<Object, Object>) map);
                default -> throw new ExecutionException("Unknown object types '%s'", existing);
            };
        } else {
            return toIndexable(existing);
        }
    }

    static Iterable<?> toIterable(final Object existing) {
        ExecutionException.when(existing == null, "Cannot used None as list.");
        return switch (existing) {
            case String s -> new IndexedString(s);
            case LngList arr -> arr;
            case Object[] arr -> new JavaArray(arr);
            case List<?> list -> new JavaArray(list.toArray(Object[]::new));
            case Iterable<?> it -> it;
            default -> throw new ExecutionException("Unknown list types %s", existing);
        };
    }

    static HasIndex toIndexable(final Object existing) {
        ExecutionException.when(existing == null, "Cannot used None as list.");
        return switch (existing) {
            case String s -> new IndexedString(s);
            case LngList arr -> arr;
            case Object[] arr -> new JavaArray(arr);
            case List<?> list -> new JavaArray(list.toArray(Object[]::new));
            default -> throw new ExecutionException("Unknown list types %s", existing);
        };
    }
}
