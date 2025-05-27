package ch.turic.memory;

import ch.turic.ExecutionException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The {@code JavaObject} record represents a wrapper for an object that provides dynamic field access.
 * This implementation supports interaction with fields of the wrapped object, allowing both retrieval
 * and assignment of field values. It implements the {@link HasFields} interface for dynamic field handling.
 * <p>
 * Key Features:
 * - Supports dynamic field access on an encapsulated object, including both getting and setting fields.
 * - If the wrapped object itself implements {@link HasFields}, the dynamic access is delegated to it.
 * - If the wrapped object does not implement {@link HasFields}, reflection is used for field operations.
 * - Provides a custom {@code toString()} method for better representation of the encapsulated object.
 * <p>
 * Thread safety:
 * This class does not guarantee thread safety. Concurrent access to the methods should be externally synchronized if needed.
 *
 * @param object the encapsulated object for which dynamic field access is provided
 */
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
            throw new ExecutionException("Cannot get '%s.%s' because '%s'.", object, name, e);
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
