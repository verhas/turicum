package javax0.turicum.memory;

import javax0.turicum.ExecutionException;

import java.util.Iterator;
import java.util.Objects;

/**
 * An object in the language
 */
public class LngObject implements HasFields, HasIndex, HasContext {

    private final LngClass lngClass;
    private final Context context;

    public LngObject(LngClass lngClass, Context context) {
        this.lngClass = lngClass;
        this.context = context;
    }

    @Override
    public void setField(String name, Object value) {
        context.local(name, value);
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        final var value = context.get(name);
        if (value != null) {
            return value;
        }
        if (lngClass == null) {
            return null;
        }
        return lngClass.getField(name);
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        setField(index.toString(), value);
    }

    @Override
    public Object getIndex(Object index) throws ExecutionException {
        return getField(index.toString());
    }

    @Override
    public Iterator<Object> iterator() {
        return context().frame.values().iterator();
    }

    @Override
    public Context context() {
        return context;
    }

    public LngClass lngClass() {
        return lngClass;
    }

}
