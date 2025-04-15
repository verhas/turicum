package ch.turic.memory;

import ch.turic.ExecutionException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public record JavaObject(Object object) implements HasFields {

    @Override
    public void setField(String name, Object value) {
        if (object instanceof HasFields fielder) {
            fielder.setField(name, value);
            return;
        }
        try {
            final var field = object.getClass().getField(name);
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExecutionException("Cannot set the value of the field '" + name + "' ", e);
        }
    }

    @Override
    public Object getField(String name) throws ExecutionException {
        if (object instanceof HasFields fielder) {
            return fielder.getField(name);
        }
        try {
            final var field = object.getClass().getField(name);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExecutionException("Cannot get the value of the field '" + name + "' on the value '%s' it is not an object or does not have that field.", e, object);
        }
    }

    @Override
    public Set<String> fields() {
        return Arrays.stream(object.getClass().getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        if (object == null) {
            return "null";
        }
        return switch (object) {
            case String s -> "\"" + s + "\"";
            case Character c -> "'" + c + "'";
            default -> object.toString();
        };
    }
}
