package javax0.turicum.memory;

import javax0.turicum.ExecutionException;

public record JavaObject(Object object) implements HasFields {

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
