package ch.turic.memory;

import ch.turic.ExecutionException;

import java.util.List;
import java.util.Map;

public interface LeftValue {

    HasFields getObject(Context ctx) throws ExecutionException;

    HasIndex getIndexable(Context ctx, Object indexValue) throws ExecutionException;

    void assign(Context ctx, Object value) throws ExecutionException;

    static HasFields toObject(Object existing) {
        if (existing instanceof LngObject || existing instanceof LngClass || existing instanceof LngList) {
            return (HasFields) existing;
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
                default -> throw new ExecutionException("Unknown object types '%s'", existing);
            };
        } else {
            return toIndexable(existing);
        }
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
