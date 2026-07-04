package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.Closure;
import ch.turic.commands.FunctionCall;
import ch.turic.commands.operators.Cast;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An object in the language
 */
public class LngObject implements HasFields, HasIndex, HasContext {
    public static final String TO_STRING_METHOD = "to_string";
    final private LngClass lngClass;
    final private LocalContext context;
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
    public LngObject(LngClass lngClass, LocalContext context) {
        this.lngClass = lngClass;
        this.context = context;
    }

    /**
     * Creates a new empty without class opening a new context under the provided one.
     *
     * @param context the context in which the object is created. The context of the object will be opened from
     *                this object using {@link LocalContext#open()}, thus the object does not wrap the provided
     *                context, but it is necessary to create the context of the object in the given interpreter
     *                (global context is inherited).
     * @return a new instance of LngObject with no associated class.
     */
    public static LngObject newEmpty(LocalContext context) {
        return new LngObject(null, context.open());
    }

    @Override
    public void setField(String name, Object value) {
        ExecutionException.when(pinned.get(), "You cannot change a pinned object");
        ExecutionException.when(context.isVeiled(name), "Field '%s' is veiled.", name);
        context.local(name, value);
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        ExecutionException.when(context.isVeiled(name), "Field '%s' is veiled.", name);
        return getFieldUnveiled(name);
    }

    /**
     * Field access without the veil check. It is used by the runtime's internal method dispatch:
     * a bare method call inside a method is rewritten to a {@code this}/{@code cls} field access,
     * and those must see the veiled names — a method of the object may call the veiled methods
     * and read the veiled fields.
     *
     * @param name the name of the field
     * @return the value of the field or {@code null}
     */
    public Object getFieldUnveiled(String name) throws ExecutionException {
        final var value = context.getLocal(name);
        if (value != null) {
            return value;
        }
        if (lngClass != null) {
            return lngClass.getFieldUnveiled(name);
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
        final var array = new ArrayList<>();
        for( final var key : fields() ){
            final var field = new LngList();
            field.add(key);
            field.add(getField(key));
            array.add(field);
        }
        return array.iterator();
    }

    @Override
    public LocalContext context() {
        return context;
    }

    public LngClass lngClass() {
        return lngClass;
    }

    public boolean instanceOf(LngClass lngClass) {
        if (lngClass == null) {
            return true;
        }
        return this.lngClass != null && this.lngClass.assignableTo(lngClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LngObject lngObject)) return false;
        var method = this.getField("==");
        if (method instanceof Closure lngEquals) {
            ExecutionException.when(!lngEquals.parameters().fitOperator(), "Operator methods must have exactly one argument");
            final var argValues = new FunctionCall.ArgumentEvaluated[]{new FunctionCall.ArgumentEvaluated(null, o)};
            final LocalContext ctx;
            if (lngEquals.wrapped() == null) {
                ctx = context.wrap(this.context());
            } else {
                ctx = context.wrap(lngEquals.wrapped());
                ctx.let0("this", this);
                ctx.let0("cls", this.lngClass);
            }
            FunctionCall.freezeThisAndCls(ctx);
            FunctionCall.defineArgumentsInContext(ctx, context, lngEquals.parameters(), argValues, true);
            return Cast.toBoolean(lngEquals.execute(ctx));
        }
        if (!Objects.equals(lngClass, lngObject.lngClass)) {
            return false;
        }
        final var allKeys = new HashSet<>(context.keys());
        allKeys.addAll(lngObject.fields());
        return CycleGuard.equals(this, lngObject, () -> {
            for (final var key : allKeys) {
                if (!Objects.equals(getField(key), lngObject.getField(key))) {
                    return false;
                }
            }
            return true;
        });
    }

    @Override
    public int hashCode() {
        return CycleGuard.hashCode(this, () -> {
            int result = Objects.hashCode(lngClass);
            for (var key : context.keys()) {
                if ("==".equals(key)) {
                    continue;
                }
                result = 31 * result + Objects.hashCode(getField(key));
            }
            return result;
        });
    }

    @Override
    public String toString() {
        return CycleGuard.toString(this, "{...}", () -> {
            // a veiled to_string is not accessible from the outside; use the default representation
            final var to_string = context.isVeiled(TO_STRING_METHOD) ? null : getField(TO_STRING_METHOD);
            if (to_string == null) {
                final var builder = new StringBuilder("{");
                String sep = "";
                try {
                    for (var key : context().keys()) {
                        builder.append(sep).append(key).append(": ").append(context().get(key));
                        sep = ", ";
                    }
                    builder.append("}");
                    return builder.toString();
                } catch (ConcurrentModificationException cme) {
                    throw new ExecutionException(cme, "ConcurrentModification exception while to_string() the object.");
                }
            } else {
                if (!(to_string instanceof Closure closure)) {
                    throw new ExecutionException("output handler does not have '%s()' method", TO_STRING_METHOD);
                }
                return Objects.requireNonNullElse(closure.callAsMethod(context, this, TO_STRING_METHOD), "none").toString();
            }
        });
    }

}
