package javax0.turicum.memory;

import javax0.turicum.ExecutionException;

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
            throw new ExecutionException("Cannot get the value of the field '" + name + "' ", e);
        }
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
