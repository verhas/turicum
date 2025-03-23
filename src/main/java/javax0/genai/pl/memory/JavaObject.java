package javax0.genai.pl.memory;

import javax0.genai.pl.commands.ExecutionException;

public class JavaObject implements HasFields {
    private final Object object;

    public JavaObject(Object object) {
        this.object = object;
    }

    @Override
    public void setField(String name, Object value) {
        try {
            final var field = object.getClass().getField(name);
            field.set(object,value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExecutionException("Cannot set the value of the field '" + name + "' ", e);
        }
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        try {
            final var field = object.getClass().getField(name);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExecutionException("Cannot get the value of the field '" + name + "' ", e);
        }
    }
}
