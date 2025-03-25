package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;

/**
 * An object in the language
 */
public record LngObject(LngClass lngClass, Context context) implements HasFields, HasIndex {

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
}
