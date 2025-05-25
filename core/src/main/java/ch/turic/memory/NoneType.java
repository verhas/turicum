package ch.turic.memory;

/**
 * Represents a singleton type conceptually similar to Python's `NoneType`.
 * When a function parameter, a variable or return type can be `none` then this class represents the type.
 */
public class NoneType {
    public static final NoneType INSTANCE = new NoneType();
}
