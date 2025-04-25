package ch.turic.utils;

import java.util.NoSuchElementException;

/**
 * Nullable optional is an object that can be present and still have the value to be null.
 * It is used in return values where you have to separate the result missing and null, which may not be the same.
 * It is not to avoid null pointer exception. For that you can use the JDK Optional.
 *
 * @param <T>
 */
public final class NullableOptional<T> {
    private final boolean present;
    private final T value;

    private NullableOptional(boolean present, T value) {
        this.present = present;
        this.value = value;
    }

    public static <T> NullableOptional<T> empty() {
        return new NullableOptional<>(false, null);
    }

    public static <T> NullableOptional<T> of(T value) {
        return new NullableOptional<>(true, value);
    }

    public boolean isPresent() {
        return present;
    }

    public T get() {
        if (!present) {
            throw new NoSuchElementException();
        }
        return value;
    }
}

