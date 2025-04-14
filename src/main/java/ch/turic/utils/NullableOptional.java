package ch.turic.utils;

import java.util.NoSuchElementException;

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

