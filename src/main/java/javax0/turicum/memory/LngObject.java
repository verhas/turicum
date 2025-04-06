package javax0.turicum.memory;

import javax0.turicum.ExecutionException;

import java.util.Iterator;

/**
 * An object in the language
 */
public class LngObject implements HasFields, HasIndex, HasContext {
    final LngClass lngClass;
    final Context context;

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
        final var value = context.getLocal(name);
        if (value != null) {
            return value;
        }
        if (lngClass != null) {
            return lngClass.getField(name);
        }
        return null;
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
        throw new ExecutionException("You cannot iterate over the field values.");
    }

    @Override
    public Context context() {
        return context;
    }

    public LngClass lngClass() {
        return lngClass;
    }

    public boolean instanceOf(LngClass lngClass) {
        if (lngClass == null) {
            return true;
        }
        return this.lngClass.assignableTo(lngClass);
    }

}
