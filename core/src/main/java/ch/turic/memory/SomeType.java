package ch.turic.memory;

/**
 * Represents a singleton instance that indicates the presence of a non-null value.
 * <p>
 * The {@code SomeType} class is used to represent that a value exists and is not null.
 * In Turicum language, null and "none" are equivalent and both represent absence of a value.
 * This class serves as a type marker for any value that is not null/"none".
 * Contains a single, unmodifiable {@code INSTANCE} field that provides a globally
 * accessible static instance of this type.
 */
@SuppressWarnings("InstantiationOfUtilityClass")
public class SomeType {
    /**
     * The singleton instance of {@code SomeType}.
     * Used to indicate presence of a non-null value in the type system.
     */
    public static final SomeType INSTANCE = new SomeType();

    private SomeType() {
    }
}
