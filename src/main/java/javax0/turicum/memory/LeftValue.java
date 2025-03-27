package javax0.turicum.memory;

import javax0.turicum.commands.ExecutionException;

import java.util.List;
import java.util.Map;

public interface LeftValue {

    HasFields getObject(Context ctx) throws ExecutionException;

    HasIndex getIndexable(Context ctx, Object indexValue) throws ExecutionException;

    void assign(Context ctx, Object value) throws ExecutionException;

    static HasFields toObject(Object existing) {
        return switch (existing) {
            case LngObject obj -> obj;
            case LngClass cls -> cls;
            case Map<?, ?> map -> new MapObject(Map.copyOf(map));
            default -> new JavaObject(existing);
        };
    }

    static HasIndex toIndexable(final Object existing) {
        return switch (existing) {
            case LngList arr -> arr;
            case Object[] arr -> new JavaArray(arr);
            case List<?> list -> new JavaArray(list.toArray(Object[]::new));
            default -> throw new ExecutionException("Unknown array type %s", existing);
        };
    }
}
