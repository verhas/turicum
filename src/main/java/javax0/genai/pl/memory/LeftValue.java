package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;

import java.util.Map;

public interface LeftValue {

    HasFields getObject(Context ctx) throws ExecutionException;
    HasIndex getArray(Context ctx) throws ExecutionException;
    void assign(Context ctx, Object value) throws ExecutionException;

    static HasFields toObject(Object existing){
        return switch (existing) {
            case LngObject obj -> obj;
            case LngClass cls -> cls;
            case Map<?, ?> map -> new MapObject(Map.copyOf(map));
            default -> new JavaObject(existing);
        };
    }

    static HasIndex toArray(Object existing){
        return switch (existing) {
            case LngArray arr -> arr;
            case Object[] arr -> new JavaArray(arr);
            default -> throw new ExecutionException("Unknown array type %s", existing);
        };
    }

}
