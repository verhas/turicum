package ch.turic.memory;

import ch.turic.exceptions.ExecutionException;
import ch.turic.commands.FunctionCall;

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
            Field field;
            try {
                field = object.getClass().getField(name);
            } catch (NoSuchFieldException e) {
                field = object.getClass().getDeclaredField(name);
                field.setAccessible(true);
            }
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExecutionException("Cannot set the value of the field '" + name + "' ", e);
        }
    }

    /****
     * Retrieves the value of the specified field from the wrapped object.
     * <p>
     * If the wrapped object implements {@code HasFields}, delegates to its {@code getField} method.
     * Otherwise, attempts to access the public field by name using reflection.
     *
     * @param name the name of the field to retrieve
     * @return the value of the specified field
     * @throws ExecutionException if the field does not exist or is inaccessible
     */
    @Override
    public Object getField(String name) throws ExecutionException {
        if (object instanceof HasFields fieldHaber) {
            return fieldHaber.getField(name);
        }
        try {
            Field field;
            try {
                field = object.getClass().getField(name);
            } catch (NoSuchFieldException e) {
                field = object.getClass().getDeclaredField(name);
                field.setAccessible(true);
            }
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExecutionException("Cannot get '%s.%s' because '%s'.", object, name, e);
        }
    }

    /**
     * Retrieves the value of a field or method named {@code name} from the wrapped object, using the provided context for dynamic resolution.
     *
     * <p>If the wrapped object implements {@code HasFields}, delegates to its {@code getField} method. Otherwise, attempts to resolve a method from a Turi class using the context; if found, returns the result of invoking the method. If no Turi class is available, accesses the public field by name via reflection.</p>
     *
     * @param name    the name of the field or method to retrieve
     * @param context the context used for dynamic method resolution
     * @return the value of the field or the result of the method invocation
     * @throws ExecutionException if the field or method cannot be found or accessed
     */
    public Object getField(String name, LocalContext context) throws ExecutionException {
        if (object instanceof HasFields fieldHaber) {
            return fieldHaber.getField(name);
        }
        try {
            final var turi = FunctionCall.getTuriClass(context, this);
            if (turi != null) {
                return turi.getMethod(object(), name);
            }
            Field field;
            try {
                field = object.getClass().getField(name);
            } catch (NoSuchFieldException e) {
                field = object.getClass().getDeclaredField(name);
                field.setAccessible(true);
            }
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
