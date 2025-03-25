package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;
import javax0.genai.pl.commands.operators.Cast;

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

    static HasIndex toIndexable(Object existing, Object indexValue) {
        if (Cast.isLong(indexValue)) {
            return switch (existing) {
                case LngList arr -> arr;
                case Object[] arr -> new JavaArray(arr);
                default -> throw new ExecutionException("Unknown array type %s", existing);
            };
        } else {
            return switch (existing) {
                case LngList arr -> arr;
                case Object[] arr -> new JavaArray(arr);
                default -> throw new ExecutionException("Unknown array type %s", existing);
            };

        }
    }

}
