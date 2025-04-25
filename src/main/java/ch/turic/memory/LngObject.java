package ch.turic.memory;

import ch.turic.ExecutionException;
import ch.turic.commands.Closure;
import ch.turic.commands.FunctionCall;
import ch.turic.commands.operators.Cast;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An object in the language
 */
public class LngObject implements HasFields, HasIndex, HasContext {
    final LngClass lngClass;
    final Context context;
    public final AtomicBoolean pinned = new AtomicBoolean(false);

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
        ExecutionException.when(pinned.get(), "You cannot change a pinned object");
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
    public Set<String> fields() {
        return context.keys();
    }

    @Override
    public void setIndex(Object index, Object value) throws ExecutionException {
        ExecutionException.when(pinned.get(), "You cannot change a pinned object");
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
        var method = this.getField("==");
        if (method instanceof Closure lngEquals) {
            ExecutionException.when(!lngEquals.parameters().fitOperator(), "Operator methods must have exactly one argument");
            final var argValues = new FunctionCall.ArgumentEvaluated[]{new FunctionCall.ArgumentEvaluated(null,o)};
            final Context ctx;
            if (lngEquals.wrapped() == null) {
                ctx = context.wrap(this.context());
            } else {
                ctx = context.wrap(lngEquals.wrapped());
                ctx.let0("this", this);
                ctx.let0("cls", this.lngClass);
            }
            FunctionCall.freezeThisAndCls(ctx);
            FunctionCall.defineArgumentsInContext(ctx, context,lngEquals.parameters(), argValues);
            return Cast.toBoolean(lngEquals.execute(ctx));
        }
        if (!Objects.equals(lngClass, lngObject.lngClass)) {
            return false;
        }
        final var compared = new HashSet<>();
        final var allKeys = new HashSet<>(context.keys());
        allKeys.addAll(lngObject.fields());
        compared.add(lngObject);
        compared.add(this);
        for (final var key : allKeys) {
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
            if ("==".equals(key)) {
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

    @Override
    public String toString() {
        final var builder = new StringBuilder("{");
        String sep = "";
        for (var key : context().keys()) {
            final var object = context().get(key);
            if (object != this) {
                builder.append(sep).append(key).append(": ").append(object);
            }
            sep = ", ";
        }
        builder.append("}");
        return builder.toString();
    }

}
