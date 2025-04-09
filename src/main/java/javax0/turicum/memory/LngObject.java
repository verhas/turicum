package javax0.turicum.memory;

import javax0.turicum.ExecutionException;
import javax0.turicum.commands.Closure;
import javax0.turicum.commands.operators.Cast;

import java.util.*;

/**
 * An object in the language
 */
public class LngObject implements HasFields, HasIndex, HasContext {
    final LngClass lngClass;
    final Context context;

    /**
     * Create a new object.
     *
     * @param lngClass the object will be the instance of this class. It can be {@code null} if the object is class less,
     *                 a.k.a. a simple dictionary/map.
     * @param context  is the context of the object. Must not be {@code null}, except special implementation.
     *                 This context will hold the data for the object. Usually this is a wrapping context of the context
     *                 from where the object was created.
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final var lngObject = (LngObject) o;
        if (!Objects.equals(lngClass, lngObject.lngClass)) {
            return false;
        }
        var method = lngObject.getField("equals");
        if (method instanceof Closure lngEquals) {
            return Cast.toBoolean(lngEquals.call(context(), o));
        }
        final var compared = new HashSet<>();
        compared.add(lngObject);
        compared.add(this);
        for (final var key : context.keys()) {
            final var thisField = getField(key);
            final var thatField = lngObject.getField(key);
            if (!compared.contains(thisField) && !compared.contains(thatField) && !Objects.equals(thisField, thatField)) {
                return false;
            }
            compared.add(thisField);
            compared.add(thatField);
        }
        return true;
    }

    @Override
    public int hashCode() {
        return computeHashCode(new IdentityHashMap<>());
    }

    private int computeHashCode(Map<Object, Boolean> visited) {
        if (visited.containsKey(this)) {
            return 0; // prevent infinite loop in cyclic structures
        }
        visited.put(this, true);

        int result = Objects.hashCode(lngClass);

        for (var key : context.keys()) {
            if ("equals".equals(key)) {
                continue; // skip dynamic equals override field
            }
            Object value = getField(key);
            int fieldHash;
            if (visited.containsKey(value)) {
                fieldHash = 0;
            } else if (value instanceof LngObject obj) {
                fieldHash = obj.computeHashCode(visited);
            } else {
                fieldHash = Objects.hashCode(value);
            }
            result = 31 * result + fieldHash;
        }

        return result;
    }

}
